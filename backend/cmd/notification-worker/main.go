package main

import (
	"context"
	"encoding/json"
	"log"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"

	"github.com/dearly/backend/internal/medication"
	"github.com/dearly/backend/internal/notification"
	"github.com/dearly/backend/pkg/database"
	"github.com/dearly/backend/pkg/events"
	firebasepkg "github.com/dearly/backend/pkg/firebase"
	"github.com/segmentio/kafka-go"
)

func main() {
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	db, err := database.Connect()
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()
	firebaseClients, err := firebasepkg.InitClients(ctx)
	if err != nil {
		log.Fatal(err)
	}
	service := notification.NewService(db, firebaseClients.Messaging)
	medicationService := medication.NewService(db, events.NoopPublisher{})

	brokers := strings.Split(env("KAFKA_BROKERS", "kafka:9092"), ",")
	reader := kafka.NewReader(kafka.ReaderConfig{
		Brokers: brokers, Topic: events.DomainTopic, GroupID: "notification-worker",
		MinBytes: 1, MaxBytes: 10e6, CommitInterval: time.Second,
	})
	defer reader.Close()

	go reminderLoop(ctx, service, medicationService)
	log.Printf("notification worker consuming %s", events.DomainTopic)
	for {
		message, err := reader.FetchMessage(ctx)
		if err != nil {
			if ctx.Err() != nil {
				return
			}
			log.Printf("fetch Kafka message: %v", err)
			continue
		}
		var event events.Event
		if err := json.Unmarshal(message.Value, &event); err != nil {
			log.Printf("decode event: %v", err)
			_ = reader.CommitMessages(ctx, message)
			continue
		}
		if err := service.HandleEvent(ctx, event); err != nil {
			log.Printf("handle event %s: %v", event.ID, err)
			continue
		}
		if err := reader.CommitMessages(ctx, message); err != nil {
			log.Printf("commit event %s: %v", event.ID, err)
		}
	}
}

func reminderLoop(ctx context.Context, service *notification.Service, medicationService *medication.Service) {
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()
	for {
		if err := medicationService.EnsureDoseLogs(ctx, "", 2); err != nil && ctx.Err() == nil {
			log.Printf("prepare medication logs: %v", err)
		}
		if err := service.DispatchDueReminders(ctx); err != nil && ctx.Err() == nil {
			log.Printf("dispatch reminders: %v", err)
		}
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
		}
	}
}

func env(name, fallback string) string {
	if value := os.Getenv(name); value != "" {
		return value
	}
	return fallback
}
