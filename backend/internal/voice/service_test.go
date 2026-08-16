package voice

import (
	"context"
	"errors"
	"testing"
)

func TestVerifyRejectsUnprotectedIntentBeforeReadingEnrollment(t *testing.T) {
	service := &Service{}

	_, err := service.Verify(context.Background(), "user", "ASK_TIME", "voice.wav", []byte("audio"))

	if !errors.Is(err, ErrInvalidIntent) {
		t.Fatalf("expected ErrInvalidIntent, got %v", err)
	}
}

func TestGrantHashAndIntentNormalization(t *testing.T) {
	if normalizeIntent(" mark_taken ") != IntentMarkTaken {
		t.Fatal("expected normalized protected intent")
	}
	if hashGrant("grant") != hashGrant(" grant ") {
		t.Fatal("expected surrounding whitespace to be ignored")
	}
	if hashGrant("grant") == hashGrant("different") {
		t.Fatal("different grants must not share a hash")
	}
}
