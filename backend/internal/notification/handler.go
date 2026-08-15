package notification

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
	rg.POST("/register", h.RegisterToken)
	rg.GET("", h.List)
}

func (h *Handler) RegisterToken(c *gin.Context) {
	var request struct {
		FCMToken string `json:"fcm_token" binding:"required"`
	}
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_request"})
		return
	}
	if err := h.service.RegisterToken(c.Request.Context(), auth.UserID(c), request.FCMToken); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "token_registration_failed"})
		return
	}
	c.Status(http.StatusNoContent)
}

func (h *Handler) List(c *gin.Context) {
	items, err := h.service.List(c.Request.Context(), auth.UserID(c))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "notifications_read_failed"})
		return
	}
	c.JSON(http.StatusOK, items)
}
