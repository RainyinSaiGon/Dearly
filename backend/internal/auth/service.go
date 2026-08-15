package auth

import (
	"context"
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"errors"
	"fmt"
	"strings"

	firebaseauth "firebase.google.com/go/v4/auth"
	"github.com/dearly/backend/pkg/events"
	jwtpkg "github.com/dearly/backend/pkg/jwt"
	"github.com/redis/go-redis/v9"
)

var (
	ErrInvalidRole  = errors.New("role must be ELDER or CAREGIVER")
	ErrInvalidToken = errors.New("invalid authentication token")
)

type FirebaseTokenVerifier interface {
	VerifyIDToken(ctx context.Context, idToken string) (*firebaseauth.Token, error)
}

type User struct {
	ID          string  `json:"id"`
	PhoneNumber *string `json:"phone_number,omitempty"`
	Email       *string `json:"email,omitempty"`
	Name        string  `json:"name"`
	Role        string  `json:"role"`
	AvatarURL   *string `json:"avatar_url,omitempty"`
}

type Session struct {
	AccessToken  string `json:"access_token"`
	RefreshToken string `json:"refresh_token"`
	ExpiresIn    int64  `json:"expires_in"`
	User         User   `json:"user"`
}

type Service struct {
	db       *sql.DB
	redis    *redis.Client
	firebase FirebaseTokenVerifier
	jwt      *jwtpkg.Service
	events   events.Publisher
}

func NewService(db *sql.DB, redisClient *redis.Client, firebase FirebaseTokenVerifier, jwtService *jwtpkg.Service, publisher events.Publisher) *Service {
	return &Service{db: db, redis: redisClient, firebase: firebase, jwt: jwtService, events: publisher}
}

func (s *Service) CreateSession(ctx context.Context, firebaseIDToken, requestedRole string) (*Session, error) {
	role := strings.ToUpper(strings.TrimSpace(requestedRole))
	if role != "ELDER" && role != "CAREGIVER" {
		return nil, ErrInvalidRole
	}
	token, err := s.firebase.VerifyIDToken(ctx, firebaseIDToken)
	if err != nil {
		return nil, fmt.Errorf("%w: %v", ErrInvalidToken, err)
	}

	phone := claimString(token.Claims, "phone_number")
	email := claimString(token.Claims, "email")
	name := claimString(token.Claims, "name")
	avatar := claimString(token.Claims, "picture")
	if name == "" {
		switch {
		case phone != "":
			name = phone
		case email != "":
			name = strings.Split(email, "@")[0]
		default:
			name = "Dearly user"
		}
	}

	user, err := s.upsertUser(ctx, token.UID, phone, email, name, role, avatar)
	if err != nil {
		return nil, err
	}
	if role == "ELDER" {
		if _, err := s.db.ExecContext(ctx, `INSERT INTO elder_profiles(user_id) VALUES($1) ON CONFLICT(user_id) DO NOTHING`, user.ID); err != nil {
			return nil, fmt.Errorf("create elder profile: %w", err)
		}
	}

	session, err := s.issueSession(ctx, user)
	if err != nil {
		return nil, err
	}
	_ = s.events.Publish(ctx, "auth.session.created", user.ID, map[string]any{"user_id": user.ID, "role": user.Role})
	return session, nil
}

func (s *Service) Refresh(ctx context.Context, refreshToken string) (*Session, error) {
	claims, err := s.jwt.ValidateToken(refreshToken)
	if err != nil || claims.TokenType != "refresh" {
		return nil, ErrInvalidToken
	}
	storedHash, err := s.redis.Get(ctx, refreshKey(claims.ID)).Result()
	if err != nil {
		return nil, ErrInvalidToken
	}
	if storedHash != tokenHash(refreshToken) {
		return nil, ErrInvalidToken
	}

	user, err := s.getUser(ctx, claims.UserID)
	if err != nil {
		return nil, err
	}
	_ = s.redis.Del(ctx, refreshKey(claims.ID)).Err()
	return s.issueSession(ctx, user)
}

func (s *Service) Revoke(ctx context.Context, refreshToken string) error {
	claims, err := s.jwt.ValidateToken(refreshToken)
	if err != nil || claims.TokenType != "refresh" {
		return ErrInvalidToken
	}
	return s.redis.Del(ctx, refreshKey(claims.ID)).Err()
}

func (s *Service) issueSession(ctx context.Context, user User) (*Session, error) {
	access, err := s.jwt.GenerateAccessToken(user.ID, user.Role)
	if err != nil {
		return nil, err
	}
	refresh, err := s.jwt.GenerateRefreshToken(user.ID, user.Role)
	if err != nil {
		return nil, err
	}
	claims, err := s.jwt.ValidateToken(refresh)
	if err != nil {
		return nil, err
	}
	if err := s.redis.Set(ctx, refreshKey(claims.ID), tokenHash(refresh), s.jwt.RefreshExpiry()).Err(); err != nil {
		return nil, fmt.Errorf("store refresh token: %w", err)
	}
	return &Session{
		AccessToken: access, RefreshToken: refresh,
		ExpiresIn: int64(s.jwt.AccessExpiry().Seconds()), User: user,
	}, nil
}

func (s *Service) upsertUser(ctx context.Context, uid, phone, email, name, role, avatar string) (User, error) {
	var user User
	if _, err := s.db.ExecContext(ctx, `
		UPDATE users SET firebase_uid=$1, updated_at=NOW()
		WHERE firebase_uid IS NULL AND (
			(NULLIF($2,'') IS NOT NULL AND phone_number=NULLIF($2,'')) OR
			(NULLIF($3,'') IS NOT NULL AND email=NULLIF($3,''))
		)`, uid, phone, email); err != nil {
		return User{}, fmt.Errorf("link existing Firebase user: %w", err)
	}
	err := s.db.QueryRowContext(ctx, `
		INSERT INTO users(firebase_uid, phone_number, email, name, role, avatar_url)
		VALUES($1, NULLIF($2,''), NULLIF($3,''), $4, $5, NULLIF($6,''))
		ON CONFLICT(firebase_uid) DO UPDATE SET
			phone_number=COALESCE(EXCLUDED.phone_number, users.phone_number),
			email=COALESCE(EXCLUDED.email, users.email),
			name=CASE WHEN users.name IN ('', 'Dearly user') THEN EXCLUDED.name ELSE users.name END,
			avatar_url=COALESCE(EXCLUDED.avatar_url, users.avatar_url),
			updated_at=NOW()
		RETURNING id::text, phone_number, email, name, role, avatar_url`,
		uid, phone, email, name, role, avatar,
	).Scan(&user.ID, &user.PhoneNumber, &user.Email, &user.Name, &user.Role, &user.AvatarURL)
	if err != nil {
		return User{}, fmt.Errorf("upsert user: %w", err)
	}
	return user, nil
}

func (s *Service) getUser(ctx context.Context, userID string) (User, error) {
	var user User
	err := s.db.QueryRowContext(ctx, `
		SELECT id::text, phone_number, email, name, role, avatar_url
		FROM users WHERE id=$1`, userID,
	).Scan(&user.ID, &user.PhoneNumber, &user.Email, &user.Name, &user.Role, &user.AvatarURL)
	if err != nil {
		return User{}, fmt.Errorf("get user: %w", err)
	}
	return user, nil
}

func claimString(claims map[string]interface{}, name string) string {
	value, _ := claims[name].(string)
	return value
}

func tokenHash(token string) string {
	sum := sha256.Sum256([]byte(token))
	return hex.EncodeToString(sum[:])
}

func refreshKey(jti string) string {
	return "refresh:" + jti
}
