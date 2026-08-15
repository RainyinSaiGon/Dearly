package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"

	"github.com/dearly/backend/internal/access"
	"github.com/dearly/backend/internal/auth"
	"github.com/dearly/backend/internal/contact"
	"github.com/dearly/backend/internal/medication"
	"github.com/dearly/backend/internal/notification"
	"github.com/dearly/backend/internal/user"
	"github.com/dearly/backend/internal/voice"
	"github.com/dearly/backend/pkg/database"
	"github.com/dearly/backend/pkg/events"
	firebasepkg "github.com/dearly/backend/pkg/firebase"
	jwtpkg "github.com/dearly/backend/pkg/jwt"
	redisclient "github.com/dearly/backend/pkg/redis"
	"github.com/gin-gonic/gin"
)

func main() {
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	db, err := database.Connect()
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()
	if err := database.Migrate(ctx, db, env("MIGRATIONS_PATH", "migrations")); err != nil {
		log.Fatal(err)
	}
	redisClient, err := redisclient.Connect()
	if err != nil {
		log.Fatal(err)
	}
	defer redisClient.Close()
	firebaseClients, err := firebasepkg.InitClients(ctx)
	if err != nil {
		log.Fatal(err)
	}
	jwtService, err := jwtpkg.NewService()
	if err != nil {
		log.Fatal(err)
	}

	var publisher events.Publisher = events.NoopPublisher{}
	if brokers := strings.TrimSpace(os.Getenv("KAFKA_BROKERS")); brokers != "" {
		publisher = events.NewKafkaPublisher(strings.Split(brokers, ","))
	}
	defer publisher.Close()

	authService := auth.NewService(db, redisClient, firebaseClients.Auth, jwtService, publisher)
	userService := user.NewService(db)
	contactService := contact.NewService(db, publisher)
	medicationService := medication.NewService(db, publisher)
	notificationService := notification.NewService(db, firebaseClients.Messaging)
	voiceService := voice.NewService(db, env("AI_SERVICE_URL", "http://ai-service:5000"), publisher)
	elderResolver := access.Resolver{DB: db}

	router := gin.New()
	router.Use(gin.Logger(), gin.Recovery())
	router.GET("/health", func(c *gin.Context) {
		ctx, cancel := context.WithTimeout(c.Request.Context(), 2*time.Second)
		defer cancel()
		if err := db.PingContext(ctx); err != nil {
			c.JSON(http.StatusServiceUnavailable, gin.H{"status": "degraded", "database": "unavailable"})
			return
		}
		if err := redisClient.Ping(ctx).Err(); err != nil {
			c.JSON(http.StatusServiceUnavailable, gin.H{"status": "degraded", "redis": "unavailable"})
			return
		}
		c.JSON(http.StatusOK, gin.H{"status": "ok", "service": "api"})
	})

	v1 := router.Group("/api/v1")
	auth.NewHandler(authService).RegisterRoutes(v1.Group("/auth"))
	protected := v1.Group("", auth.JWTMiddleware(jwtService))
	user.NewHandler(userService).RegisterRoutes(protected.Group("/users"))
	contact.NewHandler(contactService, elderResolver).RegisterRoutes(protected.Group("/contacts"))
	medication.NewHandler(medicationService, elderResolver).RegisterRoutes(protected.Group("/medications"))
	notification.NewHandler(notificationService).RegisterRoutes(protected.Group("/notifications"))
	voice.NewHandler(voiceService).RegisterRoutes(protected.Group("/voice"))

	server := &http.Server{
		Addr: ":" + env("SERVER_PORT", "8080"), Handler: router,
		ReadHeaderTimeout: 5 * time.Second, ReadTimeout: 30 * time.Second,
		WriteTimeout: 120 * time.Second, IdleTimeout: 60 * time.Second,
	}
	go func() {
		log.Printf("Dearly API listening on %s", server.Addr)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("serve API: %v", err)
		}
	}()
	<-ctx.Done()
	shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := server.Shutdown(shutdownCtx); err != nil {
		log.Printf("shutdown API: %v", err)
	}
}

func env(name, fallback string) string {
	if value := os.Getenv(name); value != "" {
		return value
	}
	return fallback
}
