package events

import (
	"context"
	"encoding/json"
	"time"

	"github.com/google/uuid"
	"github.com/segmentio/kafka-go"
)

const DomainTopic = "dearly.domain-events"

type Event struct {
	ID          string          `json:"id"`
	Type        string          `json:"type"`
	Version     int             `json:"version"`
	OccurredAt  time.Time       `json:"occurred_at"`
	AggregateID string          `json:"aggregate_id"`
	Data        json.RawMessage `json:"data"`
}

type Publisher interface {
	Publish(ctx context.Context, eventType, aggregateID string, payload any) error
	Close() error
}

type KafkaPublisher struct {
	writer *kafka.Writer
}

func NewKafkaPublisher(brokers []string) *KafkaPublisher {
	return &KafkaPublisher{writer: &kafka.Writer{
		Addr:         kafka.TCP(brokers...),
		Topic:        DomainTopic,
		Balancer:     &kafka.Hash{},
		RequiredAcks: kafka.RequireAll,
		Async:        false,
		WriteTimeout: 5 * time.Second,
		ReadTimeout:  5 * time.Second,
	}}
}

func (p *KafkaPublisher) Publish(ctx context.Context, eventType, aggregateID string, payload any) error {
	data, err := json.Marshal(payload)
	if err != nil {
		return err
	}
	eventBytes, err := json.Marshal(Event{
		ID:          uuid.NewString(),
		Type:        eventType,
		Version:     1,
		OccurredAt:  time.Now().UTC(),
		AggregateID: aggregateID,
		Data:        data,
	})
	if err != nil {
		return err
	}
	return p.writer.WriteMessages(ctx, kafka.Message{Key: []byte(aggregateID), Value: eventBytes})
}

func (p *KafkaPublisher) Close() error {
	return p.writer.Close()
}

type NoopPublisher struct{}

func (NoopPublisher) Publish(context.Context, string, string, any) error { return nil }
func (NoopPublisher) Close() error                                       { return nil }
