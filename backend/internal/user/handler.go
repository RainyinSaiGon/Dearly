package user

import (
	"errors"
	"net/http"

	"github.com/dearly/backend/internal/auth"
	"github.com/gin-gonic/gin"
)

type Handler struct {
	service *Service
}

func NewHandler(service *Service) *Handler { return &Handler{service: service} }

func (h *Handler) RegisterRoutes(rg *gin.RouterGroup) {
	rg.GET("/me", h.GetMe)
	rg.PUT("/me", h.UpdateMe)
	rg.GET("/me/voice-preferences", h.GetVoicePreferences)
	rg.PUT("/me/voice-preferences", h.UpdateVoicePreferences)
	rg.GET("/me/elders", h.GetElders)
	rg.POST("/me/link-code", h.CreateLinkCode)
	rg.POST("/me/elders", h.LinkElder)
	rg.DELETE("/me/elders/:elderID", h.UnlinkElder)
}

func (h *Handler) GetVoicePreferences(c *gin.Context) {
	preferences, err := h.service.GetVoicePreferences(c.Request.Context(), auth.UserID(c))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "voice_preferences_read_failed"})
		return
	}
	c.JSON(http.StatusOK, preferences)
}

func (h *Handler) UpdateVoicePreferences(c *gin.Context) {
	var updates VoicePreferencesUpdates
	if err := c.ShouldBindJSON(&updates); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_voice_preferences"})
		return
	}
	preferences, err := h.service.UpdateVoicePreferences(c.Request.Context(), auth.UserID(c), updates)
	if err != nil {
		if errors.Is(err, ErrInvalidVoicePreferences) {
			c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_voice_preferences"})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": "voice_preferences_update_failed"})
		return
	}
	c.JSON(http.StatusOK, preferences)
}

func (h *Handler) CreateLinkCode(c *gin.Context) {
	if auth.Role(c) != "ELDER" {
		c.JSON(http.StatusForbidden, gin.H{"error": "elder_required"})
		return
	}
	code, err := h.service.CreateCaregiverLinkCode(c.Request.Context(), auth.UserID(c))
	if err != nil {
		status := http.StatusInternalServerError
		if errors.Is(err, ErrRoleNotAllowed) {
			status = http.StatusForbidden
		}
		c.JSON(status, gin.H{"error": "link_code_create_failed"})
		return
	}
	c.JSON(http.StatusCreated, code)
}

func (h *Handler) LinkElder(c *gin.Context) {
	if auth.Role(c) != "CAREGIVER" {
		c.JSON(http.StatusForbidden, gin.H{"error": "caregiver_required"})
		return
	}
	var request struct {
		LinkCode string `json:"link_code" binding:"required"`
	}
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_request"})
		return
	}
	profile, err := h.service.LinkElder(c.Request.Context(), auth.UserID(c), request.LinkCode)
	if err != nil {
		status := http.StatusInternalServerError
		code := "elder_link_failed"
		if errors.Is(err, ErrInvalidLinkCode) {
			status, code = http.StatusBadRequest, "invalid_or_expired_link_code"
		} else if errors.Is(err, ErrRoleNotAllowed) {
			status, code = http.StatusForbidden, "caregiver_required"
		}
		c.JSON(status, gin.H{"error": code})
		return
	}
	c.JSON(http.StatusCreated, profile)
}

func (h *Handler) UnlinkElder(c *gin.Context) {
	if auth.Role(c) != "CAREGIVER" {
		c.JSON(http.StatusForbidden, gin.H{"error": "caregiver_required"})
		return
	}
	err := h.service.UnlinkElder(c.Request.Context(), auth.UserID(c), c.Param("elderID"))
	if err != nil {
		status := http.StatusInternalServerError
		if errors.Is(err, ErrLinkNotFound) {
			status = http.StatusNotFound
		}
		c.JSON(status, gin.H{"error": "elder_unlink_failed"})
		return
	}
	c.Status(http.StatusNoContent)
}

func (h *Handler) GetMe(c *gin.Context) {
	profile, err := h.service.GetByID(c.Request.Context(), auth.UserID(c))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "profile_read_failed"})
		return
	}
	c.JSON(http.StatusOK, profile)
}

func (h *Handler) UpdateMe(c *gin.Context) {
	var updates Updates
	if err := c.ShouldBindJSON(&updates); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_request"})
		return
	}
	profile, err := h.service.Update(c.Request.Context(), auth.UserID(c), updates)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "profile_update_failed"})
		return
	}
	c.JSON(http.StatusOK, profile)
}

func (h *Handler) GetElders(c *gin.Context) {
	if auth.Role(c) != "CAREGIVER" {
		c.JSON(http.StatusForbidden, gin.H{"error": "caregiver_required"})
		return
	}
	profiles, err := h.service.GetEldersForCaregiver(c.Request.Context(), auth.UserID(c))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "elders_read_failed"})
		return
	}
	c.JSON(http.StatusOK, profiles)
}
