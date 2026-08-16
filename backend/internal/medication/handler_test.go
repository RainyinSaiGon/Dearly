package medication

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/dearly/backend/internal/auth"
	"github.com/gin-gonic/gin"
)

type fixedResolver struct{}

func (fixedResolver) Resolve(*gin.Context, string) (string, error) { return "elder-id", nil }

type rejectingGrantConsumer struct {
	called bool
}

func (consumer *rejectingGrantConsumer) ConsumeVerificationGrant(
	context.Context,
	string,
	string,
	string,
) error {
	consumer.called = true
	return errors.New("invalid grant")
}

func TestMarkTakenRejectsRequestWithoutVerificationGrant(t *testing.T) {
	gin.SetMode(gin.TestMode)
	consumer := &rejectingGrantConsumer{}
	handler := &Handler{resolver: fixedResolver{}, grants: consumer}
	recorder := httptest.NewRecorder()
	request := httptest.NewRequest(
		http.MethodPost,
		"/medications/medication-id/taken",
		strings.NewReader(`{"scheduled_time":"2026-08-16T08:00:00Z"}`),
	)
	request.Header.Set("Content-Type", "application/json")
	context, _ := gin.CreateTestContext(recorder)
	context.Request = request
	context.Params = gin.Params{{Key: "id", Value: "medication-id"}}
	context.Set(auth.ContextUserID, "elder-id")
	context.Set(auth.ContextRole, "ELDER")

	handler.MarkTaken(context)

	if recorder.Code != http.StatusForbidden {
		t.Fatalf("expected 403, got %d", recorder.Code)
	}
	if !consumer.called {
		t.Fatal("expected the verification grant to be checked")
	}
}

func TestMarkTakenRejectsCaregiverBeforeConsumingGrant(t *testing.T) {
	gin.SetMode(gin.TestMode)
	consumer := &rejectingGrantConsumer{}
	handler := &Handler{resolver: fixedResolver{}, grants: consumer}
	recorder := httptest.NewRecorder()
	request := httptest.NewRequest(
		http.MethodPost,
		"/medications/medication-id/taken",
		strings.NewReader(`{"scheduled_time":"2026-08-16T08:00:00Z"}`),
	)
	request.Header.Set("Content-Type", "application/json")
	request.Header.Set("X-Voice-Grant", "caregiver-grant")
	context, _ := gin.CreateTestContext(recorder)
	context.Request = request
	context.Params = gin.Params{{Key: "id", Value: "medication-id"}}
	context.Set(auth.ContextUserID, "caregiver-id")
	context.Set(auth.ContextRole, "CAREGIVER")

	handler.MarkTaken(context)

	if recorder.Code != http.StatusForbidden {
		t.Fatalf("expected 403, got %d", recorder.Code)
	}
	if consumer.called {
		t.Fatal("caregiver request must not consume an elder voice grant")
	}
}
