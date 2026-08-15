package user

import (
	"context"
	"database/sql"
	"fmt"
)

type Profile struct {
	ID                    string  `json:"id"`
	PhoneNumber           *string `json:"phone_number,omitempty"`
	Email                 *string `json:"email,omitempty"`
	Name                  string  `json:"name"`
	Age                   *int    `json:"age,omitempty"`
	City                  *string `json:"city,omitempty"`
	Role                  string  `json:"role"`
	AvatarURL             *string `json:"avatar_url,omitempty"`
	HealthStatusAuto      *string `json:"health_status_auto,omitempty"`
	HealthStatusOverride  *string `json:"health_status_override,omitempty"`
	HealthStatusEffective *string `json:"health_status_effective,omitempty"`
	MissedDosesCount      int     `json:"missed_doses_count"`
}

type Updates struct {
	Name      *string `json:"name"`
	Age       *int    `json:"age"`
	City      *string `json:"city"`
	AvatarURL *string `json:"avatar_url"`
	FCMToken  *string `json:"fcm_token"`
}

type Service struct {
	db *sql.DB
}

func NewService(db *sql.DB) *Service { return &Service{db: db} }

func (s *Service) GetByID(ctx context.Context, userID string) (*Profile, error) {
	var profile Profile
	err := s.db.QueryRowContext(ctx, `
		SELECT u.id::text, u.phone_number, u.email, u.name, u.age, u.city, u.role,
		       u.avatar_url, ep.health_status_override,
		       COUNT(ml.id) FILTER (
		         WHERE ml.scheduled_time::date=CURRENT_DATE
		           AND ml.status='PENDING' AND ml.scheduled_time < NOW()
		       )::int
		FROM users u
		LEFT JOIN elder_profiles ep ON ep.user_id=u.id
		LEFT JOIN medications m ON m.elder_id=u.id
		LEFT JOIN medication_logs ml ON ml.medication_id=m.id
		WHERE u.id=$1
		GROUP BY u.id, ep.health_status_override`,
		userID,
	).Scan(
		&profile.ID, &profile.PhoneNumber, &profile.Email, &profile.Name, &profile.Age,
		&profile.City, &profile.Role, &profile.AvatarURL, &profile.HealthStatusOverride,
		&profile.MissedDosesCount,
	)
	if err != nil {
		return nil, fmt.Errorf("get profile: %w", err)
	}
	if profile.Role == "ELDER" {
		auto := healthStatus(profile.MissedDosesCount)
		profile.HealthStatusAuto = &auto
		if profile.HealthStatusOverride != nil {
			profile.HealthStatusEffective = profile.HealthStatusOverride
		} else {
			profile.HealthStatusEffective = &auto
		}
	}
	return &profile, nil
}

func (s *Service) Update(ctx context.Context, userID string, updates Updates) (*Profile, error) {
	_, err := s.db.ExecContext(ctx, `
		UPDATE users SET
			name=COALESCE($2, name),
			age=COALESCE($3, age),
			city=COALESCE($4, city),
			avatar_url=COALESCE($5, avatar_url),
			fcm_token=COALESCE($6, fcm_token),
			updated_at=NOW()
		WHERE id=$1`,
		userID, updates.Name, updates.Age, updates.City, updates.AvatarURL, updates.FCMToken,
	)
	if err != nil {
		return nil, fmt.Errorf("update profile: %w", err)
	}
	return s.GetByID(ctx, userID)
}

func (s *Service) GetEldersForCaregiver(ctx context.Context, caregiverID string) ([]Profile, error) {
	rows, err := s.db.QueryContext(ctx, `
		SELECT elder_id::text FROM caregiver_elder_links
		WHERE caregiver_id=$1 ORDER BY created_at`, caregiverID)
	if err != nil {
		return nil, fmt.Errorf("list elders: %w", err)
	}
	defer rows.Close()
	var profiles []Profile
	for rows.Next() {
		var elderID string
		if err := rows.Scan(&elderID); err != nil {
			return nil, err
		}
		profile, err := s.GetByID(ctx, elderID)
		if err != nil {
			return nil, err
		}
		profiles = append(profiles, *profile)
	}
	return profiles, rows.Err()
}

func healthStatus(missed int) string {
	switch {
	case missed >= 5:
		return "CRITICAL"
	case missed >= 2:
		return "WARNING"
	default:
		return "NORMAL"
	}
}
