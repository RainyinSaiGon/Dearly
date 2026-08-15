package user

import (
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
	rg.GET("/me/elders", h.GetElders)
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
