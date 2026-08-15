package medication

import (
	"context"
	"errors"
	"net/http"

	"github.com/gin-gonic/gin"
)

type elderResolver interface {
	Resolve(c *gin.Context, requested string) (string, error)
}

type Handler struct {
	service  *Service
	resolver elderResolver
}

func NewHandler(service *Service, resolver elderResolver) *Handler {
	return &Handler{service: service, resolver: resolver}
}

func (h *Handler) RegisterRoutes(rg *gin.RouterGroup) {
	rg.GET("", h.List)
	rg.POST("", h.Create)
	rg.PUT("/:id", h.Update)
	rg.DELETE("/:id", h.Delete)
	rg.POST("/:id/taken", h.MarkTaken)
	rg.POST("/:id/snooze", h.Snooze)
}

func (h *Handler) List(c *gin.Context) {
	elderID, err := h.resolver.Resolve(c, c.Query("elder_id"))
	if err != nil {
		c.JSON(http.StatusForbidden, gin.H{"error": "elder_access_denied"})
		return
	}
	items, err := h.service.List(c.Request.Context(), elderID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "medications_read_failed"})
		return
	}
	c.JSON(http.StatusOK, items)
}

func (h *Handler) Create(c *gin.Context) {
	var input Input
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_request"})
		return
	}
	elderID, err := h.resolver.Resolve(c, input.ElderID)
	if err != nil {
		c.JSON(http.StatusForbidden, gin.H{"error": "elder_access_denied"})
		return
	}
	item, err := h.service.Create(c.Request.Context(), elderID, input)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "medication_create_failed", "message": err.Error()})
		return
	}
	c.JSON(http.StatusCreated, item)
}

func (h *Handler) Update(c *gin.Context) {
	var input Input
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_request"})
		return
	}
	elderID, err := h.resolver.Resolve(c, input.ElderID)
	if err != nil {
		c.JSON(http.StatusForbidden, gin.H{"error": "elder_access_denied"})
		return
	}
	item, err := h.service.Update(c.Request.Context(), c.Param("id"), elderID, input)
	if err != nil {
		writeError(c, "medication_update_failed", err)
		return
	}
	c.JSON(http.StatusOK, item)
}

func (h *Handler) Delete(c *gin.Context) {
	elderID, err := h.resolver.Resolve(c, c.Query("elder_id"))
	if err != nil {
		c.JSON(http.StatusForbidden, gin.H{"error": "elder_access_denied"})
		return
	}
	if err := h.service.Delete(c.Request.Context(), c.Param("id"), elderID); err != nil {
		writeError(c, "medication_delete_failed", err)
		return
	}
	c.Status(http.StatusNoContent)
}

type doseRequest struct {
	ElderID       string `json:"elder_id"`
	ScheduledTime string `json:"scheduled_time" binding:"required"`
}

func (h *Handler) MarkTaken(c *gin.Context) {
	h.changeDose(c, h.service.MarkTaken)
}

func (h *Handler) Snooze(c *gin.Context) {
	h.changeDose(c, h.service.Snooze)
}

func (h *Handler) changeDose(c *gin.Context, change func(c context.Context, medicationID, elderID, scheduledTime string) error) {
	var request doseRequest
	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_request"})
		return
	}
	elderID, err := h.resolver.Resolve(c, request.ElderID)
	if err != nil {
		c.JSON(http.StatusForbidden, gin.H{"error": "elder_access_denied"})
		return
	}
	if err := change(c.Request.Context(), c.Param("id"), elderID, request.ScheduledTime); err != nil {
		writeError(c, "dose_update_failed", err)
		return
	}
	c.Status(http.StatusNoContent)
}

func writeError(c *gin.Context, code string, err error) {
	status := http.StatusBadRequest
	if errors.Is(err, ErrNotFound) {
		status = http.StatusNotFound
	}
	c.JSON(status, gin.H{"error": code, "message": err.Error()})
}
