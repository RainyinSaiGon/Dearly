package voice

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

// Handler handles HTTP requests for the voice assistant module.
type Handler struct {
	// TODO(W4-W5): Inject VoiceService
	// service *Service
}

func NewHandler() *Handler { return &Handler{} }

func (h *Handler) RegisterRoutes(rg *gin.RouterGroup) {
	rg.POST("/enroll", h.Enroll)
	rg.DELETE("/enroll/:userId", h.ResetEnrollment)
	rg.POST("/query", h.Query)
	rg.POST("/verify", h.Verify)
}

// Enroll accepts audio samples for speaker enrollment.
// Called once per phrase (5 total) during the onboarding flow.
//
// TODO(W4):
//  1. Bind multipart form: audio file + phrase_index (0–4)
//  2. Validate phrase_index range and that audio is a valid wav/m4a file
//  3. Forward audio to ai-service POST /enroll (multipart)
//  4. Receive embedding vector from ai-service
//  5. UPSERT into voice_enrollments (user_id, phrase_index, embedding_vector)
//     ON CONFLICT (user_id, phrase_index) DO UPDATE SET embedding_vector = $1
//  6. After all 5 phrases are enrolled (phrase_index 0–4), compute and store average embedding
//  7. Return { phrase_index, enrolled_count, total_required: 5 }
func (h *Handler) Enroll(c *gin.Context) {
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}

// ResetEnrollment deletes all voice enrollment data for a user.
//
// TODO(W4):
//  1. Verify requesting user is the enrolled user or their caregiver
//  2. DELETE FROM voice_enrollments WHERE user_id = $1
//  3. Return 204 No Content
func (h *Handler) ResetEnrollment(c *gin.Context) {
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}

// Query is the main voice assistant endpoint.
// Accepts an audio recording and returns the assistant's spoken response.
//
// TODO(W5):
//  1. Bind multipart audio file
//  2. Forward to ai-service POST /query
//  3. ai-service runs:  audio → Whisper ASR → GPT-4o intent → (SV if needed) → execute → TTS
//  4. ai-service returns: { transcript, intent, response_text, response_audio_url, sv_required, sv_passed }
//  5. If sv_required && !sv_passed: return 403 with TTS of "Xác minh giọng nói thất bại"
//  6. Log the query + response for caregiver's activity view
//  7. Return response_audio_url or stream audio bytes back to client
func (h *Handler) Query(c *gin.Context) {
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}

// Verify runs a one-shot speaker verification check.
// Used when a protected action is detected mid-conversation.
//
// TODO(W5):
//  1. Bind multipart audio file
//  2. Load user's average enrollment embedding from voice_enrollments
//  3. Forward audio + stored embedding to ai-service POST /verify
//  4. ai-service returns { passed: bool, similarity_score: float }
//  5. Return { passed, similarity_score } — let client decide next action
func (h *Handler) Verify(c *gin.Context) {
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}
