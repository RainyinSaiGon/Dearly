package voice

import (
	"io"
	"net/http"
	"strconv"

	"github.com/dearly/backend/internal/auth"
	"github.com/gin-gonic/gin"
)

const maxAudioBytes = 20 << 20

type Handler struct {
	service *Service
}

func NewHandler(service *Service) *Handler { return &Handler{service: service} }

func (h *Handler) RegisterRoutes(rg *gin.RouterGroup) {
	rg.POST("/enroll", h.Enroll)
	rg.DELETE("/enroll", h.ResetEnrollment)
	rg.POST("/query", h.Query)
	rg.POST("/verify", h.Verify)
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
		c.JSON(http.StatusBadGateway, gin.H{"error": "voice_query_failed", "message": err.Error()})
		return
	}
	c.JSON(http.StatusOK, result)
}

func (h *Handler) Verify(c *gin.Context) {
	filename, audio, ok := readAudio(c)
	if !ok {
		return
	}
	passed, score, err := h.service.Verify(c.Request.Context(), auth.UserID(c), filename, audio)
	if err != nil {
		c.JSON(http.StatusBadGateway, gin.H{"error": "voice_verification_failed", "message": err.Error()})
		return
	}
	c.JSON(http.StatusOK, gin.H{"passed": passed, "score": score})
}

func readAudio(c *gin.Context) (string, []byte, bool) {
	file, header, err := c.Request.FormFile("audio")
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "audio_required"})
		return "", nil, false
	}
	defer file.Close()
	audio, err := io.ReadAll(io.LimitReader(file, maxAudioBytes+1))
	if err != nil || len(audio) == 0 || len(audio) > maxAudioBytes {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_audio"})
		return "", nil, false
	}
	return header.Filename, audio, true
}
