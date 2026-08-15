package auth

import (
	"net/http"
	"strings"

	jwtpkg "github.com/dearly/backend/pkg/jwt"
	"github.com/gin-gonic/gin"
)

const (
	ContextUserID = "userID"
	ContextRole   = "role"
)

func JWTMiddleware(jwtService *jwtpkg.Service) gin.HandlerFunc {
	return func(c *gin.Context) {
		header := c.GetHeader("Authorization")
		if !strings.HasPrefix(header, "Bearer ") {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "missing_or_invalid_token"})
			return
		}
		claims, err := jwtService.ValidateToken(strings.TrimSpace(strings.TrimPrefix(header, "Bearer ")))
		if err != nil || claims.TokenType != "access" {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "invalid_or_expired_token"})
			return
		}
		c.Set(ContextUserID, claims.UserID)
		c.Set(ContextRole, claims.Role)
		c.Next()
	}
}

func UserID(c *gin.Context) string {
	value, _ := c.Get(ContextUserID)
	userID, _ := value.(string)
	return userID
}

func Role(c *gin.Context) string {
	value, _ := c.Get(ContextRole)
	role, _ := value.(string)
	return role
}
