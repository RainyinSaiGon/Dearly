package main

import (
	"log"
	"net/http"
	"os"

	"github.com/gin-gonic/gin"
)

func main() {
	port := os.Getenv("SERVER_PORT")
	if port == "" {
		port = "8080"
	}

	r := gin.Default()

	// ─── Health check ──────────────────────────────────────────────
	r.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	})

	// ─── API v1 routes ─────────────────────────────────────────────
	v1 := r.Group("/api/v1")
	{
		// Auth
		auth := v1.Group("/auth")
		{
			auth.POST("/send-otp", notImplemented)
			auth.POST("/verify-otp", notImplemented)
			auth.POST("/google", notImplemented)
			auth.POST("/refresh", notImplemented)
		}

		// Users
		users := v1.Group("/users")
		{
			users.GET("/me", notImplemented)
			users.PUT("/me", notImplemented)
			users.GET("/:id/elders", notImplemented)
		}

		// Voice
		voice := v1.Group("/voice")
		{
			voice.POST("/enroll", notImplemented)
			voice.DELETE("/enroll/:userId", notImplemented)
			voice.POST("/query", notImplemented)
			voice.POST("/verify", notImplemented)
		}

		// Contacts
		contacts := v1.Group("/contacts")
		{
			contacts.GET("", notImplemented)
			contacts.POST("", notImplemented)
			contacts.PUT("/:id", notImplemented)
			contacts.DELETE("/:id", notImplemented)
		}

		// Medications
		medications := v1.Group("/medications")
		{
			medications.GET("", notImplemented)
			medications.POST("", notImplemented)
			medications.PUT("/:id", notImplemented)
			medications.DELETE("/:id", notImplemented)
			medications.POST("/:id/taken", notImplemented)
		}

		// Notifications
		notifications := v1.Group("/notifications")
		{
			notifications.POST("/register", notImplemented)
			notifications.GET("", notImplemented)
		}
	}

	log.Printf("Dearly API server starting on :%s", port)
	if err := r.Run(":" + port); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}

// notImplemented returns 501 for all stub routes.
func notImplemented(c *gin.Context) {
	c.JSON(http.StatusNotImplemented, gin.H{
		"error":   "not_implemented",
		"message": "This endpoint is not yet implemented",
	})
}
