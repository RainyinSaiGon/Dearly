package jwtpkg

import (
	"fmt"
	"os"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
)

// Claims represents the payload of a Dearly JWT.
//
// TODO(W2): Add any extra claims needed (e.g., device_id for multi-device logout).
type Claims struct {
	UserID    string `json:"user_id"`
	Role      string `json:"role"`
	TokenType string `json:"token_type"`
	jwt.RegisteredClaims
}

// Service provides JWT generation and validation.
type Service struct {
	secret        []byte
	accessExpiry  time.Duration
	refreshExpiry time.Duration
}

// NewService creates a JWTService from environment variables.
//
// TODO(W2): Call this from main.go and inject the result into AuthService.
func NewService() (*Service, error) {
	secret := os.Getenv("JWT_SECRET")
	if secret == "" {
		return nil, fmt.Errorf("JWT_SECRET is not set")
	}

	accessExpiry, err := durationFromEnv("JWT_ACCESS_EXPIRY", 15*time.Minute)
	if err != nil {
		return nil, err
	}
	refreshExpiry, err := durationFromEnv("JWT_REFRESH_EXPIRY", 7*24*time.Hour)
	if err != nil {
		return nil, err
	}
	return &Service{
		secret:        []byte(secret),
		accessExpiry:  accessExpiry,
		refreshExpiry: refreshExpiry,
	}, nil
}

func (s *Service) GenerateAccessToken(userID, role string) (string, error) {
	return s.generate(userID, role, "access", s.accessExpiry)
}

func (s *Service) GenerateRefreshToken(userID, role string) (string, error) {
	return s.generate(userID, role, "refresh", s.refreshExpiry)
}

func (s *Service) ValidateToken(tokenStr string) (*Claims, error) {
	claims := &Claims{}
	token, err := jwt.ParseWithClaims(tokenStr, claims, func(token *jwt.Token) (interface{}, error) {
		if token.Method != jwt.SigningMethodHS256 {
			return nil, fmt.Errorf("unexpected signing method %q", token.Method.Alg())
		}
		return s.secret, nil
	}, jwt.WithIssuer("dearly-api"), jwt.WithValidMethods([]string{jwt.SigningMethodHS256.Alg()}))
	if err != nil || !token.Valid {
		return nil, fmt.Errorf("invalid token: %w", err)
	}
	return claims, nil
}

func (s *Service) RefreshExpiry() time.Duration {
	return s.refreshExpiry
}

func (s *Service) AccessExpiry() time.Duration {
	return s.accessExpiry
}

func (s *Service) generate(userID, role, tokenType string, expiry time.Duration) (string, error) {
	now := time.Now().UTC()
	claims := Claims{
		UserID:    userID,
		Role:      role,
		TokenType: tokenType,
		RegisteredClaims: jwt.RegisteredClaims{
			Issuer:    "dearly-api",
			Subject:   userID,
			ID:        uuid.NewString(),
			IssuedAt:  jwt.NewNumericDate(now),
			NotBefore: jwt.NewNumericDate(now),
			ExpiresAt: jwt.NewNumericDate(now.Add(expiry)),
		},
	}
	return jwt.NewWithClaims(jwt.SigningMethodHS256, claims).SignedString(s.secret)
}

func durationFromEnv(name string, fallback time.Duration) (time.Duration, error) {
	raw := os.Getenv(name)
	if raw == "" {
		return fallback, nil
	}
	value, err := time.ParseDuration(raw)
	if err != nil {
		return 0, fmt.Errorf("%s must be a Go duration: %w", name, err)
	}
	return value, nil
}
