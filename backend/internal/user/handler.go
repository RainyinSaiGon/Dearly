package user

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

// Handler handles HTTP requests for the user/profile module.
type Handler struct {
	// TODO(W2): Inject UserService
	// service *Service
}

// NewHandler creates a new user handler.
func NewHandler() *Handler {
	return &Handler{}
}

// RegisterRoutes wires user endpoints to the provided router group.
// TODO(W2): Call this from main.go for the /api/v1/users group.
func (h *Handler) RegisterRoutes(rg *gin.RouterGroup) {
	rg.GET("/me", h.GetMe)
	rg.PUT("/me", h.UpdateMe)
	rg.GET("/:id/elders", h.GetElders)
}

// GetMe returns the authenticated user's profile.
//
// TODO(W2):
//  1. Extract userID from gin context (set by JWTMiddleware)
//  2. Query DB: SELECT * FROM users WHERE id = $1
//  3. If role=ELDER, also join elder_profiles for health_status_override
//  4. Compute health_status_auto from today's missed medication_logs
//  5. Return combined profile JSON
func (h *Handler) GetMe(c *gin.Context) {
	// TODO(W2): implement
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}

// UpdateMe updates name, avatar, city, fcm_token for the authenticated user.
//
// TODO(W2):
//  1. Extract userID from context
//  2. Bind and validate request body (name, avatar_url, city, fcm_token)
//  3. UPDATE users SET ... WHERE id = $1
//  4. Return updated user profile
func (h *Handler) UpdateMe(c *gin.Context) {
	// TODO(W2): implement
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}

// GetElders returns the list of elders managed by a given caregiver.
//
// TODO(W3):
//  1. Validate that the requesting user is the caregiver with the given :id
//     (or an admin). Reject with 403 if not authorized.
//  2. Query: SELECT u.* FROM users u
//            JOIN caregiver_elder_links l ON u.id = l.elder_id
//            WHERE l.caregiver_id = $1
//  3. For each elder, compute health_status_effective
//  4. Return list of elder profiles
func (h *Handler) GetElders(c *gin.Context) {
	// TODO(W3): implement
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}
