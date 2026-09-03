package voice

import (
	"errors"
	"io"
	"net/http"
	"strconv"

	"github.com/dearly/backend/internal/auth"
	"github.com/gin-gonic/gin"
)

const maxAudioBytes = 20 << 20
const minAudioBytes = 4 << 10

type Handler struct {
	service *Service
}

func NewHandler(service *Service) *Handler { return &Handler{service: service} }

func (h *Handler) RegisterRoutes(rg *gin.RouterGroup) {
	rg.POST("/enroll", h.Enroll)
	rg.DELETE("/enroll", h.ResetEnrollment)
	rg.POST("/query", h.Query)
	rg.POST("/verify", h.Verify)
	rg.GET("/verification-audit", h.VerificationAudit)
}

// RegisterPublicRoutes exposes only the deliberately safe public assistant.
// Enrollment, identification, protected actions, and personal data remain
// behind the authenticated /voice routes.
func (h *Handler) RegisterPublicRoutes(rg *gin.RouterGroup) {
	rg.POST("/query", h.QueryPublic)
}

func (h *Handler) Enroll(c *gin.Context) {
	index, err := strconv.Atoi(c.PostForm("phrase_index"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_phrase_index"})
		return
	}
	filename, audio, ok := readAudio(c)
	if !ok {
		return
	}
	count, err := h.service.EnrollPhrase(c.Request.Context(), auth.UserID(c), index, filename, audio)
	if err != nil {
		var aiError *AIServiceError
		if errors.As(err, &aiError) && aiError.StatusCode == http.StatusUnprocessableEntity {
			c.JSON(http.StatusUnprocessableEntity, gin.H{"error": "speech_not_detected", "message": aiError.Message})
			return
		}
		c.JSON(http.StatusBadGateway, gin.H{"error": "voice_enrollment_failed", "message": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"phrase_index": index, "enrolled_count": count, "total_required": 5})
}

func (h *Handler) ResetEnrollment(c *gin.Context) {
	if err := h.service.Reset(c.Request.Context(), auth.UserID(c)); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "voice_reset_failed"})
		return
	}
	c.Status(http.StatusNoContent)
}

func (h *Handler) Query(c *gin.Context) {
	filename, audio, ok := readAudio(c)
	if !ok {
		return
	}
	result, err := h.service.Query(c.Request.Context(), auth.UserID(c), filename, audio)
	if err != nil {
		var aiError *AIServiceError
		if errors.As(err, &aiError) && aiError.StatusCode == http.StatusUnprocessableEntity {
			c.JSON(http.StatusUnprocessableEntity, gin.H{"error": "speech_not_detected", "message": aiError.Message})
			return
		}
		c.JSON(http.StatusBadGateway, gin.H{"error": "voice_query_failed", "message": err.Error()})
		return
	}
	c.JSON(http.StatusOK, result)
}

func (h *Handler) QueryPublic(c *gin.Context) {
	filename, audio, ok := readAudio(c)
	if !ok {
		return
	}
	result, err := h.service.PublicQuery(c.Request.Context(), filename, audio)
	if err != nil {
		writeQueryError(c, err)
		return
	}
	c.JSON(http.StatusOK, result)
}

func (h *Handler) Verify(c *gin.Context) {
	intent := c.PostForm("intent")
	filename, audio, ok := readAudio(c)
	if !ok {
		return
	}
	result, err := h.service.Verify(c.Request.Context(), auth.UserID(c), intent, filename, audio)
	if err != nil {
		if errors.Is(err, ErrInvalidIntent) {
			c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_protected_intent"})
			return
		}
		if errors.Is(err, ErrVerificationRateLimited) {
			c.JSON(http.StatusTooManyRequests, gin.H{"error": "voice_verification_rate_limited"})
			return
		}
		if errors.Is(err, ErrVoiceReplayDetected) {
			c.JSON(http.StatusConflict, gin.H{"error": "voice_recording_replayed"})
			return
		}
		c.JSON(http.StatusBadGateway, gin.H{"error": "voice_verification_failed", "message": err.Error()})
		return
	}
	c.JSON(http.StatusOK, result)
}

func (h *Handler) VerificationAudit(c *gin.Context) {
	entries, err := h.service.VerificationAudit(c.Request.Context(), auth.UserID(c), 20)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "voice_verification_audit_failed"})
		return
	}
	c.JSON(http.StatusOK, entries)
}

func writeQueryError(c *gin.Context, err error) {
	var aiError *AIServiceError
	if errors.As(err, &aiError) && aiError.StatusCode == http.StatusUnprocessableEntity {
		c.JSON(http.StatusUnprocessableEntity, gin.H{"error": "speech_not_detected", "message": aiError.Message})
		return
	}
	c.JSON(http.StatusBadGateway, gin.H{"error": "voice_query_failed", "message": err.Error()})
}

func readAudio(c *gin.Context) (string, []byte, bool) {
	file, header, err := c.Request.FormFile("audio")
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "audio_required"})
		return "", nil, false
	}
	defer file.Close()
	audio, err := io.ReadAll(io.LimitReader(file, maxAudioBytes+1))
	if err != nil || len(audio) < minAudioBytes || len(audio) > maxAudioBytes {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_audio"})
		return "", nil, false
	}
	return header.Filename, audio, true
}
