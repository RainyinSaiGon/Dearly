package contact

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"strings"

	"github.com/dearly/backend/pkg/events"
)

var ErrNotFound = errors.New("contact not found")

type Contact struct {
	ID           string  `json:"id"`
	ElderID      string  `json:"elder_id"`
	Nickname     string  `json:"nickname"`
	FullName     string  `json:"full_name"`
	PhoneNumber  string  `json:"phone_number"`
	Relationship string  `json:"relationship"`
	CallMethod   string  `json:"call_method"`
	AvatarURL    *string `json:"avatar_url,omitempty"`
}

type Input struct {
	ElderID      string `json:"elder_id"`
	Nickname     string `json:"nickname"`
	FullName     string `json:"full_name" binding:"required"`
	PhoneNumber  string `json:"phone_number" binding:"required"`
	Relationship string `json:"relationship"`
	CallMethod   string `json:"call_method"`
}

type Service struct {
	db     *sql.DB
	events events.Publisher
}

func NewService(db *sql.DB, publisher events.Publisher) *Service {
	return &Service{db: db, events: publisher}
}

func (s *Service) List(ctx context.Context, elderID string) ([]Contact, error) {
	rows, err := s.db.QueryContext(ctx, `
		SELECT id::text, elder_id::text, COALESCE(nickname,''), full_name,
		       phone_number, COALESCE(relationship,''), call_method
		FROM contacts WHERE elder_id=$1
		ORDER BY COALESCE(NULLIF(nickname,''), full_name)`, elderID)
	if err != nil {
		return nil, fmt.Errorf("list contacts: %w", err)
	}
	defer rows.Close()
	contacts := make([]Contact, 0)
	for rows.Next() {
		var contact Contact
		if err := rows.Scan(&contact.ID, &contact.ElderID, &contact.Nickname, &contact.FullName,
			&contact.PhoneNumber, &contact.Relationship, &contact.CallMethod); err != nil {
			return nil, err
		}
		contacts = append(contacts, contact)
	}
	return contacts, rows.Err()
}

func (s *Service) Create(ctx context.Context, elderID string, input Input) (*Contact, error) {
	if err := validate(&input); err != nil {
		return nil, err
	}
	var contact Contact
	err := s.db.QueryRowContext(ctx, `
		INSERT INTO contacts(elder_id, nickname, full_name, phone_number, relationship, call_method)
		VALUES($1, NULLIF($2,''), $3, $4, NULLIF($5,''), $6)
		RETURNING id::text, elder_id::text, COALESCE(nickname,''), full_name,
		          phone_number, COALESCE(relationship,''), call_method`,
		elderID, input.Nickname, input.FullName, input.PhoneNumber, input.Relationship, input.CallMethod,
	).Scan(&contact.ID, &contact.ElderID, &contact.Nickname, &contact.FullName,
		&contact.PhoneNumber, &contact.Relationship, &contact.CallMethod)
	if err != nil {
		return nil, fmt.Errorf("create contact: %w", err)
	}
	_ = s.events.Publish(ctx, "contact.created", contact.ID, contact)
	return &contact, nil
}

func (s *Service) Update(ctx context.Context, contactID, elderID string, input Input) (*Contact, error) {
	if err := validate(&input); err != nil {
		return nil, err
	}
	var contact Contact
	err := s.db.QueryRowContext(ctx, `
		UPDATE contacts SET nickname=NULLIF($3,''), full_name=$4, phone_number=$5,
			relationship=NULLIF($6,''), call_method=$7, updated_at=NOW()
		WHERE id=$1 AND elder_id=$2
		RETURNING id::text, elder_id::text, COALESCE(nickname,''), full_name,
		          phone_number, COALESCE(relationship,''), call_method`,
		contactID, elderID, input.Nickname, input.FullName, input.PhoneNumber,
		input.Relationship, input.CallMethod,
	).Scan(&contact.ID, &contact.ElderID, &contact.Nickname, &contact.FullName,
		&contact.PhoneNumber, &contact.Relationship, &contact.CallMethod)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrNotFound
	}
	if err != nil {
		return nil, fmt.Errorf("update contact: %w", err)
	}
	_ = s.events.Publish(ctx, "contact.updated", contact.ID, contact)
	return &contact, nil
}

func (s *Service) Delete(ctx context.Context, contactID, elderID string) error {
	result, err := s.db.ExecContext(ctx, `DELETE FROM contacts WHERE id=$1 AND elder_id=$2`, contactID, elderID)
	if err != nil {
		return fmt.Errorf("delete contact: %w", err)
	}
	count, _ := result.RowsAffected()
	if count == 0 {
		return ErrNotFound
	}
	_ = s.events.Publish(ctx, "contact.deleted", contactID, map[string]string{"elder_id": elderID})
	return nil
}

func validate(input *Input) error {
	input.FullName = strings.TrimSpace(input.FullName)
	input.PhoneNumber = strings.TrimSpace(input.PhoneNumber)
	input.CallMethod = strings.ToUpper(strings.TrimSpace(input.CallMethod))
	if input.CallMethod == "" {
		input.CallMethod = "PHONE"
	}
	if input.FullName == "" || input.PhoneNumber == "" {
		return errors.New("full_name and phone_number are required")
	}
	if input.CallMethod != "PHONE" && input.CallMethod != "ZALO_VIDEO" {
		return errors.New("call_method must be PHONE or ZALO_VIDEO")
	}
	return nil
}
