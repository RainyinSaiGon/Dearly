package auth

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

// Handler handles all HTTP requests for the auth module.
type Handler struct {
	// TODO(W2): Inject AuthService here
	// service *Service
}

// NewHandler creates a new auth handler.
// TODO(W2): Accept *Service as parameter once service is implemented.
func NewHandler() *Handler {
	return &Handler{}
}

// RegisterRoutes wires auth endpoints to the provided router group.
// TODO(W2): Call this from main.go instead of the inline route stubs.
func (h *Handler) RegisterRoutes(rg *gin.RouterGroup) {
	rg.POST("/send-otp", h.SendOTP)
	rg.POST("/verify-otp", h.VerifyOTP)
	rg.POST("/google", h.GoogleSignIn)
	rg.POST("/refresh", h.RefreshToken)
}

// SendOTP accepts a phone number and sends an OTP via Firebase Phone Auth.
//
// TODO(W2):
//  1. Validate phone number format (Vietnamese: 0[3|5|7|8|9]xxxxxxxx)
//  2. Call Firebase Admin SDK to generate a custom token or trigger SMS via client SDK
//  3. Store OTP attempt in Redis with TTL=5min, max 3 attempts
//  4. Return 200 OK on success, 429 on rate limit exceeded
func (h *Handler) SendOTP(c *gin.Context) {
	// TODO(W2): implement
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}

// VerifyOTP validates a 6-digit OTP and issues JWT access + refresh tokens.
//
// TODO(W2):
//  1. Validate the Firebase ID token returned by the client SDK after OTP verification
//  2. Upsert user record in PostgreSQL (create if first time, update fcm_token)
//  3. Generate JWT access token (15min) and refresh token (7 days)
//  4. Store refresh token hash in Redis for revocation support
//  5. Return tokens + user profile
func (h *Handler) VerifyOTP(c *gin.Context) {
	// TODO(W2): implement
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}

// GoogleSignIn accepts a Google ID token from the Android client and issues JWTs.
//
// TODO(W2):
//  1. Verify the Google ID token using Firebase Admin SDK
//  2. Upsert user record (phone may be missing — prompt client to collect it)
//  3. Generate and return JWT access + refresh tokens
func (h *Handler) GoogleSignIn(c *gin.Context) {
	// TODO(W2): implement
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}

// RefreshToken issues a new access token given a valid refresh token.
//
// TODO(W2):
//  1. Parse and validate the refresh token JWT
//  2. Check refresh token is not revoked (look up hash in Redis)
//  3. Issue a new access token (15min)
//  4. Optionally rotate the refresh token (sliding window)
func (h *Handler) RefreshToken(c *gin.Context) {
	// TODO(W2): implement
	c.JSON(http.StatusNotImplemented, gin.H{"error": "not_implemented"})
}
