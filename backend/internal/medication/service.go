package medication

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"sort"
	"strings"
	"time"

	"github.com/dearly/backend/pkg/events"
)

var ErrNotFound = errors.New("medication not found")

type DoseLog struct {
	ID             string  `json:"id"`
	MedicationID   string  `json:"medication_id"`
	MedicationName string  `json:"medication_name"`
	ScheduledTime  string  `json:"scheduled_time"`
	Status         string  `json:"status"`
	TakenAt        *string `json:"taken_at,omitempty"`
}

type Medication struct {
	ID              string    `json:"id"`
	ElderID         string    `json:"elder_id"`
	Name            string    `json:"name"`
	Dosage          string    `json:"dosage"`
	FrequencyPerDay int       `json:"frequency_per_day"`
	TimeSlots       []string  `json:"time_slots"`
	Notes           *string   `json:"notes,omitempty"`
	TodayLogs       []DoseLog `json:"today_logs"`
}

type Input struct {
	ElderID         string   `json:"elder_id"`
	Name            string   `json:"name" binding:"required"`
	Dosage          string   `json:"dosage"`
	FrequencyPerDay int      `json:"frequency_per_day" binding:"required"`
	TimeSlots       []string `json:"time_slots" binding:"required"`
	Notes           *string  `json:"notes"`
}

type Service struct {
	db       *sql.DB
	events   events.Publisher
	location *time.Location
}

func NewService(db *sql.DB, publisher events.Publisher) *Service {
	locationName := os.Getenv("APP_TIMEZONE")
	if locationName == "" {
		locationName = "Asia/Ho_Chi_Minh"
	}
	location, err := time.LoadLocation(locationName)
	if err != nil {
		location = time.UTC
	}
	return &Service{db: db, events: publisher, location: location}
}

func (s *Service) List(ctx context.Context, elderID string) ([]Medication, error) {
	rows, err := s.db.QueryContext(ctx, `
		SELECT id::text, elder_id::text, name, COALESCE(dosage,''), frequency_per_day,
		       time_slots, notes
		FROM medications WHERE elder_id=$1 ORDER BY name`, elderID)
	if err != nil {
		return nil, fmt.Errorf("list medications: %w", err)
	}
	defer rows.Close()
	medications := make([]Medication, 0)
	for rows.Next() {
		var medication Medication
		var slotsJSON []byte
		if err := rows.Scan(&medication.ID, &medication.ElderID, &medication.Name, &medication.Dosage,
			&medication.FrequencyPerDay, &slotsJSON, &medication.Notes); err != nil {
			return nil, err
		}
		if err := json.Unmarshal(slotsJSON, &medication.TimeSlots); err != nil {
			return nil, err
		}
		logs, err := s.todayLogs(ctx, medication)
		if err != nil {
			return nil, err
		}
		medication.TodayLogs = logs
		medications = append(medications, medication)
	}
	return medications, rows.Err()
}

func (s *Service) Create(ctx context.Context, elderID string, input Input) (*Medication, error) {
	if err := validate(&input); err != nil {
		return nil, err
	}
	slotsJSON, _ := json.Marshal(input.TimeSlots)
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()

	var medication Medication
	err = tx.QueryRowContext(ctx, `
		INSERT INTO medications(elder_id, name, dosage, frequency_per_day, time_slots, notes)
		VALUES($1,$2,NULLIF($3,''),$4,$5,$6)
		RETURNING id::text, elder_id::text, name, COALESCE(dosage,''), frequency_per_day, notes`,
		elderID, input.Name, input.Dosage, input.FrequencyPerDay, slotsJSON, input.Notes,
	).Scan(&medication.ID, &medication.ElderID, &medication.Name, &medication.Dosage,
		&medication.FrequencyPerDay, &medication.Notes)
	if err != nil {
		return nil, fmt.Errorf("create medication: %w", err)
	}
	medication.TimeSlots = input.TimeSlots
	if err := s.createTodayLogs(ctx, tx, medication); err != nil {
		return nil, err
	}
	if err := tx.Commit(); err != nil {
		return nil, err
	}
	medication.TodayLogs, _ = s.todayLogs(ctx, medication)
	_ = s.events.Publish(ctx, "medication.created", medication.ID, medication)
	return &medication, nil
}

func (s *Service) Update(ctx context.Context, medicationID, elderID string, input Input) (*Medication, error) {
	if err := validate(&input); err != nil {
		return nil, err
	}
	slotsJSON, _ := json.Marshal(input.TimeSlots)
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()
	var medication Medication
	err = tx.QueryRowContext(ctx, `
		UPDATE medications SET name=$3, dosage=NULLIF($4,''), frequency_per_day=$5,
			time_slots=$6, notes=$7, updated_at=NOW()
		WHERE id=$1 AND elder_id=$2
		RETURNING id::text, elder_id::text, name, COALESCE(dosage,''), frequency_per_day, notes`,
		medicationID, elderID, input.Name, input.Dosage, input.FrequencyPerDay, slotsJSON, input.Notes,
	).Scan(&medication.ID, &medication.ElderID, &medication.Name, &medication.Dosage,
		&medication.FrequencyPerDay, &medication.Notes)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrNotFound
	}
	if err != nil {
		return nil, err
	}
	medication.TimeSlots = input.TimeSlots
	if _, err := tx.ExecContext(ctx, `
		DELETE FROM medication_logs
		WHERE medication_id=$1 AND scheduled_time::date=CURRENT_DATE AND status='PENDING'`, medicationID); err != nil {
		return nil, err
	}
	if err := s.createTodayLogs(ctx, tx, medication); err != nil {
		return nil, err
	}
	if err := tx.Commit(); err != nil {
		return nil, err
	}
	medication.TodayLogs, _ = s.todayLogs(ctx, medication)
	_ = s.events.Publish(ctx, "medication.updated", medication.ID, medication)
	return &medication, nil
}

func (s *Service) Delete(ctx context.Context, medicationID, elderID string) error {
	result, err := s.db.ExecContext(ctx, `DELETE FROM medications WHERE id=$1 AND elder_id=$2`, medicationID, elderID)
	if err != nil {
		return err
	}
	count, _ := result.RowsAffected()
	if count == 0 {
		return ErrNotFound
	}
	_ = s.events.Publish(ctx, "medication.deleted", medicationID, map[string]string{"elder_id": elderID})
	return nil
}

func (s *Service) MarkTaken(ctx context.Context, medicationID, elderID, scheduledTime string) error {
	scheduled, err := time.Parse(time.RFC3339, scheduledTime)
	if err != nil {
		return errors.New("scheduled_time must be RFC3339")
	}
	var medName string
	err = s.db.QueryRowContext(ctx, `SELECT name FROM medications WHERE id=$1 AND elder_id=$2`, medicationID, elderID).Scan(&medName)
	if errors.Is(err, sql.ErrNoRows) {
		return ErrNotFound
	}
	if err != nil {
		return err
	}
	_, err = s.db.ExecContext(ctx, `
		INSERT INTO medication_logs(medication_id, scheduled_time, taken_at, status)
		VALUES($1,$2,NOW(),'TAKEN')
		ON CONFLICT(medication_id, scheduled_time) DO UPDATE
		SET status='TAKEN', taken_at=NOW(), snoozed_until=NULL`,
		medicationID, scheduled,
	)
	if err != nil {
		return err
	}
	_ = s.events.Publish(ctx, "medication.taken", medicationID, map[string]any{
		"medication_id": medicationID, "medication_name": medName,
		"elder_id": elderID, "scheduled_time": scheduled,
	})
	return nil
}

func (s *Service) Snooze(ctx context.Context, medicationID, elderID, scheduledTime string) error {
	scheduled, err := time.Parse(time.RFC3339, scheduledTime)
	if err != nil {
		return errors.New("scheduled_time must be RFC3339")
	}
	result, err := s.db.ExecContext(ctx, `
		UPDATE medication_logs ml SET status='SNOOZED', snoozed_until=NOW()+INTERVAL '10 minutes',
			notification_sent_at=NULL
		FROM medications m
		WHERE ml.medication_id=m.id AND m.id=$1 AND m.elder_id=$2 AND ml.scheduled_time=$3`,
		medicationID, elderID, scheduled,
	)
	if err != nil {
		return err
	}
	count, _ := result.RowsAffected()
	if count == 0 {
		return ErrNotFound
	}
	_ = s.events.Publish(ctx, "medication.snoozed", medicationID, map[string]any{
		"medication_id": medicationID, "elder_id": elderID, "scheduled_time": scheduled,
	})
	return nil
}

func (s *Service) createTodayLogs(ctx context.Context, tx *sql.Tx, medication Medication) error {
	now := time.Now().In(s.location)
	for _, slot := range medication.TimeSlots {
		scheduled, err := time.ParseInLocation("15:04", slot, s.location)
		if err != nil {
			return err
		}
		at := time.Date(now.Year(), now.Month(), now.Day(), scheduled.Hour(), scheduled.Minute(), 0, 0, s.location)
		if _, err := tx.ExecContext(ctx, `
			INSERT INTO medication_logs(medication_id, scheduled_time, status)
			VALUES($1,$2,'PENDING')
			ON CONFLICT(medication_id, scheduled_time) DO NOTHING`, medication.ID, at); err != nil {
			return err
		}
	}
	return nil
}

func (s *Service) todayLogs(ctx context.Context, medication Medication) ([]DoseLog, error) {
	rows, err := s.db.QueryContext(ctx, `
		SELECT id::text, scheduled_time, status, taken_at
		FROM medication_logs
		WHERE medication_id=$1 AND scheduled_time::date=CURRENT_DATE
		ORDER BY scheduled_time`, medication.ID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	logs := make([]DoseLog, 0)
	for rows.Next() {
		var id, status string
		var scheduled time.Time
		var takenAt sql.NullTime
		if err := rows.Scan(&id, &scheduled, &status, &takenAt); err != nil {
			return nil, err
		}
		log := DoseLog{ID: id, MedicationID: medication.ID, MedicationName: medication.Name,
			ScheduledTime: scheduled.UTC().Format(time.RFC3339), Status: status}
		if takenAt.Valid {
			value := takenAt.Time.UTC().Format(time.RFC3339)
			log.TakenAt = &value
		}
		logs = append(logs, log)
	}
	return logs, rows.Err()
}

func validate(input *Input) error {
	input.Name = strings.TrimSpace(input.Name)
	if input.Name == "" {
		return errors.New("name is required")
	}
	if input.FrequencyPerDay < 1 || input.FrequencyPerDay > 12 || len(input.TimeSlots) != input.FrequencyPerDay {
		return errors.New("frequency_per_day must match time_slots length and be between 1 and 12")
	}
	seen := map[string]bool{}
	for _, slot := range input.TimeSlots {
		if _, err := time.Parse("15:04", slot); err != nil {
			return fmt.Errorf("invalid time slot %q; expected HH:mm", slot)
		}
		if seen[slot] {
			return fmt.Errorf("duplicate time slot %q", slot)
		}
		seen[slot] = true
	}
	sort.Strings(input.TimeSlots)
	return nil
}
