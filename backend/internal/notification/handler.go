package notification

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

// Handler handles FCM token registration and notification listing.
type Handler struct {
	// TODO(W3): Inject NotificationService
}

func NewHandler() *Handler { return &Handler{} }

func (h *Handler) RegisterRoutes(rg *gin.RouterGroup) {
	rg.POST("/register", h.RegisterToken)
	rg.GET("", h.List)
}

// RegisterToken saves or updates the FCM device token for a user.
// Called by the Android app on every launch or token refresh.
//
// TODO(W3):
//  1. Bind JSON: { fcm_token: string }
//  2. UPDATE users SET fcm_token=$1, updated_at=NOW() WHERE id=$2
//  3. Return 200 OK
func (h *Handler) RegisterToken(c *gin.Context) {
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}

// List returns recent notifications for the authenticated user.
//
// TODO(W3):
//  1. For now, return medication reminders from medication_logs
//     WHERE scheduled_time >= NOW() - interval '24h'
//  2. Future: add a dedicated notifications table if needed
func (h *Handler) List(c *gin.Context) {
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}
