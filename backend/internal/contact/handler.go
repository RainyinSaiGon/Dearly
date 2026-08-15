package contact

import (
	"errors"
	"net/http"

	"github.com/gin-gonic/gin"
)

type Handler struct {
	service *Service
	db      elderResolver
}

type elderResolver interface {
	Resolve(c *gin.Context, requested string) (string, error)
}

func NewHandler(service *Service, resolver elderResolver) *Handler {
	return &Handler{service: service, db: resolver}
}

func (h *Handler) RegisterRoutes(rg *gin.RouterGroup) {
	rg.GET("", h.List)
	rg.POST("", h.Create)
	rg.PUT("/:id", h.Update)
	rg.DELETE("/:id", h.Delete)
}

func (h *Handler) List(c *gin.Context) {
	elderID, err := h.db.Resolve(c, c.Query("elder_id"))
	if err != nil {
		c.JSON(http.StatusForbidden, gin.H{"error": "elder_access_denied"})
		return
	}
	contacts, err := h.service.List(c.Request.Context(), elderID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "contacts_read_failed"})
		return
	}
	c.JSON(http.StatusOK, contacts)
}

func (h *Handler) Create(c *gin.Context) {
	var input Input
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_request"})
		return
	}
	elderID, err := h.db.Resolve(c, input.ElderID)
	if err != nil {
		c.JSON(http.StatusForbidden, gin.H{"error": "elder_access_denied"})
		return
	}
	created, err := h.service.Create(c.Request.Context(), elderID, input)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "contact_create_failed", "message": err.Error()})
		return
	}
	c.JSON(http.StatusCreated, created)
}

func (h *Handler) Update(c *gin.Context) {
	var input Input
	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid_request"})
		return
	}
	elderID, err := h.db.Resolve(c, input.ElderID)
	if err != nil {
		c.JSON(http.StatusForbidden, gin.H{"error": "elder_access_denied"})
		return
	}
	updated, err := h.service.Update(c.Request.Context(), c.Param("id"), elderID, input)
	if err != nil {
		status := http.StatusBadRequest
		if errors.Is(err, ErrNotFound) {
			status = http.StatusNotFound
		}
		c.JSON(status, gin.H{"error": "contact_update_failed", "message": err.Error()})
		return
	}
	c.JSON(http.StatusOK, updated)
}

func (h *Handler) Delete(c *gin.Context) {
	elderID, err := h.db.Resolve(c, c.Query("elder_id"))
	if err != nil {
		c.JSON(http.StatusForbidden, gin.H{"error": "elder_access_denied"})
		return
	}
	if err := h.service.Delete(c.Request.Context(), c.Param("id"), elderID); err != nil {
		status := http.StatusInternalServerError
		if errors.Is(err, ErrNotFound) {
			status = http.StatusNotFound
		}
		c.JSON(status, gin.H{"error": "contact_delete_failed"})
		return
	}
	c.Status(http.StatusNoContent)
}
