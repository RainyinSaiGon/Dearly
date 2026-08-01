package auth

import (
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
)

// JWTMiddleware validates the Authorization: Bearer <token> header.
//
// TODO(W2):
//  1. Extract token from "Authorization: Bearer <token>" header
//  2. Validate token with pkg/jwt (signature, expiry, issuer)
//  3. Attach decoded user claims (userID, role) to the gin context
//  4. Return 401 Unauthorized if token is missing, malformed, or expired
//
// Usage in main.go:
//
//	protected := v1.Group("/", JWTMiddleware())
func JWTMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" || !strings.HasPrefix(authHeader, "Bearer ") {
			// TODO(W2): replace with real JWT validation
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{
				"error": "missing_or_invalid_token",
			})
			return
		}

		// TODO(W2): parse and validate the JWT
		// tokenStr := strings.TrimPrefix(authHeader, "Bearer ")
		// claims, err := jwtService.ValidateToken(tokenStr)
		// if err != nil { c.AbortWithStatusJSON(401, ...) }
		// c.Set("userID", claims.UserID)
		// c.Set("role", claims.Role)

		c.Next()
	}
}
