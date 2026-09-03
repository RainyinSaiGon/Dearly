package auth

import (
	"errors"
	"net/http"

	"github.com/gin-gonic/gin"
)

type Handler struct {
	service *Service
}

func NewHandler(service *Service) *Handler {
	return &Handler{service: service}
}

func (h *Handler) RegisterRoutes(rg *gin.RouterGroup) {
	rg.POST("/session", h.CreateSession)
	rg.POST("/refresh", h.RefreshToken)
	rg.POST("/logout", h.Logout)
}

type sessionRequest struct {
	FirebaseIDToken string `json:"firebase_id_token" binding:"required"`
	Role            string `json:"role"`
}

func (h *Handler) CreateSession(c *gin.Context) {
	var request sessionRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_request", "message": err.Error()})
		return
	}
	session, err := h.service.CreateSession(c.Request.Context(), request.FirebaseIDToken, request.Role)
	if err != nil {
		status := http.StatusInternalServerError
		code := "session_failed"
		if errors.Is(err, ErrInvalidRole) {
			status, code = http.StatusBadRequest, "invalid_role"
		} else if errors.Is(err, ErrRoleSelectionRequired) {
			status, code = http.StatusConflict, "role_selection_required"
		} else if errors.Is(err, ErrInvalidToken) {
			status, code = http.StatusUnauthorized, "invalid_firebase_token"
		}
		c.JSON(status, gin.H{"error": code, "message": err.Error()})
		return
	}
	c.JSON(http.StatusOK, session)
}

type refreshRequest struct {
	RefreshToken string `json:"refresh_token" binding:"required"`
}

func (h *Handler) RefreshToken(c *gin.Context) {
	var request refreshRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_request"})
		return
	}
	session, err := h.service.Refresh(c.Request.Context(), request.RefreshToken)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "invalid_refresh_token"})
		return
	}
	c.JSON(http.StatusOK, session)
}

func (h *Handler) Logout(c *gin.Context) {
	var request refreshRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_request"})
		return
	}
	if err := h.service.Revoke(c.Request.Context(), request.RefreshToken); err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "invalid_refresh_token"})
		return
	}
	c.Status(http.StatusNoContent)
}
