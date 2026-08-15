package jwtpkg

import (
	"testing"
	"time"
)

func TestGenerateAndValidateAccessToken(t *testing.T) {
	t.Setenv("JWT_SECRET", "test-secret-with-enough-entropy")
	t.Setenv("JWT_ACCESS_EXPIRY", "2m")
	t.Setenv("JWT_REFRESH_EXPIRY", "24h")

	service, err := NewService()
	if err != nil {
		t.Fatal(err)
	}
	token, err := service.GenerateAccessToken("user-123", "ELDER")
	if err != nil {
		t.Fatal(err)
	}
	claims, err := service.ValidateToken(token)
	if err != nil {
		t.Fatal(err)
	}
	if claims.UserID != "user-123" || claims.Role != "ELDER" || claims.TokenType != "access" {
		t.Fatalf("unexpected claims: %#v", claims)
	}
	if remaining := time.Until(claims.ExpiresAt.Time); remaining < time.Minute || remaining > 3*time.Minute {
		t.Fatalf("unexpected expiry: %s", remaining)
	}
}

func TestRefreshTokenHasUniqueJTI(t *testing.T) {
	t.Setenv("JWT_SECRET", "test-secret-with-enough-entropy")
	service, err := NewService()
	if err != nil {
		t.Fatal(err)
	}
	first, _ := service.GenerateRefreshToken("user-123", "CAREGIVER")
	second, _ := service.GenerateRefreshToken("user-123", "CAREGIVER")
	firstClaims, _ := service.ValidateToken(first)
	secondClaims, _ := service.ValidateToken(second)
	if firstClaims.ID == secondClaims.ID {
		t.Fatal("refresh tokens must have unique JWT IDs")
	}
	if firstClaims.TokenType != "refresh" {
		t.Fatalf("expected refresh token, got %s", firstClaims.TokenType)
	}
}

func TestNewServiceRejectsInvalidDuration(t *testing.T) {
	t.Setenv("JWT_SECRET", "test-secret-with-enough-entropy")
	t.Setenv("JWT_ACCESS_EXPIRY", "tomorrow")
	if _, err := NewService(); err == nil {
		t.Fatal("expected invalid duration error")
	}
}
