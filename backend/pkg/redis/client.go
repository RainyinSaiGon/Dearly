package redisclient

import (
	"context"
	"fmt"
	"os"

	"github.com/redis/go-redis/v9"
)

// Connect creates a Redis client using REDIS_URL from the environment.
//
// TODO(W2):
//  1. Call this from main.go during startup
//  2. Pass the *redis.Client into AuthService (OTP rate-limiting + refresh token store)
//  3. Future: also used by notification scheduler (Asynq queue)
func Connect() (*redis.Client, error) {
	redisURL := os.Getenv("REDIS_URL")
	if redisURL == "" {
		return nil, fmt.Errorf("REDIS_URL environment variable is not set")
	}

	opts, err := redis.ParseURL(redisURL)
	if err != nil {
		return nil, fmt.Errorf("invalid REDIS_URL: %w", err)
	}

	client := redis.NewClient(opts)

	// TODO(W2): Add health check with configurable timeout
	if err := client.Ping(context.Background()).Err(); err != nil {
		return nil, fmt.Errorf("redis ping failed: %w", err)
	}

	return client, nil
}
