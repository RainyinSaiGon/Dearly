package jwtpkg

import (
	"fmt"
	"os"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

// Claims represents the payload of a Dearly JWT.
//
// TODO(W2): Add any extra claims needed (e.g., device_id for multi-device logout).
type Claims struct {
	UserID string `json:"user_id"`
	Role   string `json:"role"` // "ELDER" | "CAREGIVER"
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

	// TODO(W2): Parse JWT_ACCESS_EXPIRY and JWT_REFRESH_EXPIRY from env
	// (e.g., "15m", "168h") using time.ParseDuration
	return &Service{
		secret:        []byte(secret),
		accessExpiry:  15 * time.Minute,
		refreshExpiry: 7 * 24 * time.Hour,
	}, nil
}

// GenerateAccessToken signs a short-lived JWT for API access.
//
// TODO(W2): Implement using golang-jwt/jwt/v5.
func (s *Service) GenerateAccessToken(userID, role string) (string, error) {
	// TODO(W2): implement
	// claims := Claims{UserID: userID, Role: role, RegisteredClaims: ...}
	// return jwt.NewWithClaims(jwt.SigningMethodHS256, claims).SignedString(s.secret)
	return "", fmt.Errorf("not implemented")
}

// GenerateRefreshToken signs a long-lived JWT for token rotation.
//
// TODO(W2): Same pattern as GenerateAccessToken but with refreshExpiry.
func (s *Service) GenerateRefreshToken(userID string) (string, error) {
	// TODO(W2): implement
	return "", fmt.Errorf("not implemented")
}

// ValidateToken parses and verifies a JWT string, returning its claims.
//
// TODO(W2):
//  1. Parse token with jwt.ParseWithClaims
//  2. Validate signing method is HMAC
//  3. Return Claims on success, wrapped error on failure
func (s *Service) ValidateToken(tokenStr string) (*Claims, error) {
	// TODO(W2): implement
	return nil, fmt.Errorf("not implemented")
}
