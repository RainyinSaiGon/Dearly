package contact

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

// Handler handles HTTP requests for the contact (call list) module.
type Handler struct {
	// TODO(W3): Inject ContactService
	// service *Service
}

func NewHandler() *Handler { return &Handler{} }

func (h *Handler) RegisterRoutes(rg *gin.RouterGroup) {
	rg.GET("", h.List)
	rg.POST("", h.Create)
	rg.PUT("/:id", h.Update)
	rg.DELETE("/:id", h.Delete)
}

// List returns all contacts for the authenticated elder.
//
// TODO(W3):
//  1. Extract elderID from JWT context (role must be ELDER or CAREGIVER acting on behalf)
//  2. SELECT * FROM contacts WHERE elder_id = $1 ORDER BY nickname ASC
//  3. Return list of contact objects
func (h *Handler) List(c *gin.Context) {
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}

// Create adds a new contact for an elder.
//
// TODO(W3):
//  1. Bind JSON body: { nickname, full_name, phone_number, relationship, call_method }
//  2. Validate call_method is one of: PHONE, ZALO_VIDEO
//  3. INSERT INTO contacts (...) VALUES (...) RETURNING id
//  4. Return 201 Created with the new contact
func (h *Handler) Create(c *gin.Context) {
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}

// Update edits an existing contact by ID.
//
// TODO(W3):
//  1. Validate the contact belongs to the requesting elder (ownership check)
//  2. Bind and validate partial update fields
//  3. UPDATE contacts SET ... WHERE id = $1 AND elder_id = $2
//  4. Return updated contact
func (h *Handler) Update(c *gin.Context) {
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}

// Delete removes a contact by ID.
//
// TODO(W3):
//  1. Verify ownership (contact.elder_id == requesting userID)
//  2. DELETE FROM contacts WHERE id = $1 AND elder_id = $2
//  3. Return 204 No Content
func (h *Handler) Delete(c *gin.Context) {
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}
