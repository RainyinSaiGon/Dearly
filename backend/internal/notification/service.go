package notification

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"time"

	"firebase.google.com/go/v4/messaging"
	"github.com/dearly/backend/pkg/events"
)

type Service struct {
	db        *sql.DB
	messaging *messaging.Client
}

func NewService(db *sql.DB, messagingClient *messaging.Client) *Service {
	return &Service{db: db, messaging: messagingClient}
}

func (s *Service) RegisterToken(ctx context.Context, userID, token string) error {
	result, err := s.db.ExecContext(ctx, `UPDATE users SET fcm_token=$2, updated_at=NOW() WHERE id=$1`, userID, token)
	if err != nil {
		return err
	}
	count, _ := result.RowsAffected()
	if count == 0 {
		return sql.ErrNoRows
	}
	return nil
}

func (s *Service) List(ctx context.Context, userID string) ([]map[string]any, error) {
	rows, err := s.db.QueryContext(ctx, `
		SELECT ml.id::text, m.name, ml.scheduled_time, ml.status
		FROM medication_logs ml
		JOIN medications m ON m.id=ml.medication_id
		WHERE m.elder_id=$1 AND ml.scheduled_time >= NOW()-INTERVAL '24 hours'
		ORDER BY ml.scheduled_time DESC LIMIT 100`, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	items := make([]map[string]any, 0)
	for rows.Next() {
		var id, name, status string
		var scheduled time.Time
		if err := rows.Scan(&id, &name, &scheduled, &status); err != nil {
			return nil, err
		}
		items = append(items, map[string]any{
			"id": id, "type": "MEDICATION_REMINDER", "title": name,
			"scheduled_time": scheduled.UTC().Format(time.RFC3339), "status": status,
		})
	}
	return items, rows.Err()
}

func (s *Service) SendMedicationReminder(ctx context.Context, elderID, medicationID, medName string, scheduled time.Time) error {
	var token sql.NullString
	if err := s.db.QueryRowContext(ctx, `SELECT fcm_token FROM users WHERE id=$1`, elderID).Scan(&token); err != nil {
		return err
	}
	if !token.Valid || token.String == "" {
		return nil
	}
	_, err := s.messaging.Send(ctx, &messaging.Message{
		Token: token.String,
		Notification: &messaging.Notification{
			Title: "Đến giờ uống thuốc",
			Body:  fmt.Sprintf("%s — %s", medName, scheduled.Format("15:04")),
		},
		Data: map[string]string{
			"action": "MEDICATION_REMINDER", "medication_id": medicationID,
			"scheduled_time": scheduled.UTC().Format(time.RFC3339),
		},
		Android: &messaging.AndroidConfig{Priority: "high"},
	})
	return err
}

func (s *Service) SendToCaregivers(ctx context.Context, elderID, message string) error {
	rows, err := s.db.QueryContext(ctx, `
		SELECT u.fcm_token FROM users u
		JOIN caregiver_elder_links l ON l.caregiver_id=u.id
		WHERE l.elder_id=$1 AND u.fcm_token IS NOT NULL`, elderID)
	if err != nil {
		return err
	}
	defer rows.Close()
	for rows.Next() {
		var token string
		if err := rows.Scan(&token); err != nil {
			return err
		}
		if _, err := s.messaging.Send(ctx, &messaging.Message{
			Token:        token,
			Notification: &messaging.Notification{Title: "Dearly", Body: message},
			Data:         map[string]string{"action": "ELDER_STATUS", "elder_id": elderID},
			Android:      &messaging.AndroidConfig{Priority: "high"},
		}); err != nil {
			return err
		}
	}
	return rows.Err()
}

func (s *Service) DispatchDueReminders(ctx context.Context) error {
	rows, err := s.db.QueryContext(ctx, `
		SELECT ml.id::text, m.elder_id::text, m.id::text, m.name,
		       COALESCE(ml.snoozed_until, ml.scheduled_time)
		FROM medication_logs ml
		JOIN medications m ON m.id=ml.medication_id
		WHERE ml.status IN ('PENDING','SNOOZED')
		  AND ml.notification_sent_at IS NULL
		  AND COALESCE(ml.snoozed_until, ml.scheduled_time) <= NOW()
		  AND COALESCE(ml.snoozed_until, ml.scheduled_time) > NOW()-INTERVAL '24 hours'
		ORDER BY COALESCE(ml.snoozed_until, ml.scheduled_time)
		LIMIT 100`)
	if err != nil {
		return err
	}
	defer rows.Close()
	type reminder struct {
		logID, elderID, medicationID, name string
		scheduled                          time.Time
	}
	var reminders []reminder
	for rows.Next() {
		var item reminder
		if err := rows.Scan(&item.logID, &item.elderID, &item.medicationID, &item.name, &item.scheduled); err != nil {
			return err
		}
		reminders = append(reminders, item)
	}
	for _, item := range reminders {
		if err := s.SendMedicationReminder(ctx, item.elderID, item.medicationID, item.name, item.scheduled); err != nil {
			log.Printf("send medication reminder %s: %v", item.logID, err)
			continue
		}
		_, _ = s.db.ExecContext(ctx, `UPDATE medication_logs SET notification_sent_at=NOW() WHERE id=$1`, item.logID)
	}
	return rows.Err()
}

func (s *Service) HandleEvent(ctx context.Context, event events.Event) error {
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return err
	}
	defer tx.Rollback()
	var exists bool
	if err := tx.QueryRowContext(ctx, `
		SELECT EXISTS(SELECT 1 FROM processed_events WHERE consumer_name='notification-worker' AND event_id=$1)`,
		event.ID).Scan(&exists); err != nil {
		return err
	}
	if exists {
		return nil
	}
	if event.Type == "medication.taken" {
		var payload struct {
			ElderID        string `json:"elder_id"`
			MedicationName string `json:"medication_name"`
		}
		if err := json.Unmarshal(event.Data, &payload); err != nil {
			return err
		}
		if err := s.SendToCaregivers(ctx, payload.ElderID, fmt.Sprintf("Đã uống %s", payload.MedicationName)); err != nil {
			return err
		}
	}
	_, err = tx.ExecContext(ctx, `
		INSERT INTO processed_events(consumer_name,event_id) VALUES('notification-worker',$1)
		ON CONFLICT DO NOTHING`, event.ID)
	if err != nil {
		return err
	}
	return tx.Commit()
}
