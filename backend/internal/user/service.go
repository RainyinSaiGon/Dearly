package user

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/dearly/backend/pkg/appclock"
)

var (
	ErrInvalidLinkCode         = errors.New("link code is invalid or expired")
	ErrLinkNotFound            = errors.New("caregiver link not found")
	ErrRoleNotAllowed          = errors.New("account role cannot perform this link operation")
	ErrInvalidVoicePreferences = errors.New("voice preferences are invalid")
)

const (
	linkCodeAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
	linkCodeLength   = 8
	linkCodeLifetime = 10 * time.Minute
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

type VoicePreferences struct {
	ReminderStyle        string  `json:"reminder_style"`
	SpeechRate           float64 `json:"speech_rate"`
	PreferredContactID   *string `json:"preferred_contact_id,omitempty"`
	PreferredContactName *string `json:"preferred_contact_name,omitempty"`
	IncludeDailySchedule bool    `json:"include_daily_schedule"`
}

type VoicePreferencesUpdates struct {
	ReminderStyle        *string  `json:"reminder_style"`
	SpeechRate           *float64 `json:"speech_rate"`
	PreferredContactID   *string  `json:"preferred_contact_id"`
	IncludeDailySchedule *bool    `json:"include_daily_schedule"`
}

type Service struct {
	db       *sql.DB
	location *time.Location
}

type CaregiverLinkCode struct {
	Code      string    `json:"code"`
	ExpiresAt time.Time `json:"expires_at"`
}

func NewService(db *sql.DB) *Service {
	return &Service{db: db, location: appclock.LocationFromEnv()}
}

func (s *Service) GetByID(ctx context.Context, userID string) (*Profile, error) {
	dayStart, dayEnd := appclock.DayBounds(time.Now(), s.location)
	var profile Profile
	err := s.db.QueryRowContext(ctx, `
		SELECT u.id::text, u.phone_number, u.email, u.name, u.age, u.city, u.role,
		       u.avatar_url, ep.health_status_override,
		       COUNT(ml.id) FILTER (
		         WHERE ml.scheduled_time >= $2 AND ml.scheduled_time < $3
		           AND ml.status='PENDING' AND ml.scheduled_time < NOW()
		       )::int
		FROM users u
		LEFT JOIN elder_profiles ep ON ep.user_id=u.id
		LEFT JOIN medications m ON m.elder_id=u.id
		LEFT JOIN medication_logs ml ON ml.medication_id=m.id
		WHERE u.id=$1
		GROUP BY u.id, ep.health_status_override`,
		userID, dayStart, dayEnd,
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

func (s *Service) GetVoicePreferences(ctx context.Context, userID string) (*VoicePreferences, error) {
	preferences := &VoicePreferences{}
	err := s.db.QueryRowContext(ctx, `
		SELECT p.reminder_style, p.speech_rate, p.preferred_contact_id::text,
		       COALESCE(c.nickname, c.full_name), p.include_daily_schedule
		FROM voice_preferences p
		LEFT JOIN contacts c ON c.id=p.preferred_contact_id AND c.elder_id=p.user_id
		WHERE p.user_id=$1`, userID,
	).Scan(
		&preferences.ReminderStyle, &preferences.SpeechRate,
		&preferences.PreferredContactID, &preferences.PreferredContactName,
		&preferences.IncludeDailySchedule,
	)
	if errors.Is(err, sql.ErrNoRows) {
		return &VoicePreferences{ReminderStyle: "GENTLE", SpeechRate: 0.85, IncludeDailySchedule: true}, nil
	}
	if err != nil {
		return nil, fmt.Errorf("get voice preferences: %w", err)
	}
	return preferences, nil
}

func (s *Service) UpdateVoicePreferences(
	ctx context.Context, userID string, updates VoicePreferencesUpdates,
) (*VoicePreferences, error) {
	if updates.ReminderStyle != nil {
		style := strings.ToUpper(strings.TrimSpace(*updates.ReminderStyle))
		if style != "GENTLE" && style != "DIRECT" {
			return nil, ErrInvalidVoicePreferences
		}
		updates.ReminderStyle = &style
	}
	if updates.SpeechRate != nil && (*updates.SpeechRate < 0.5 || *updates.SpeechRate > 1.5) {
		return nil, ErrInvalidVoicePreferences
	}
	if updates.PreferredContactID != nil && strings.TrimSpace(*updates.PreferredContactID) != "" {
		var belongsToUser bool
		if err := s.db.QueryRowContext(ctx,
			`SELECT EXISTS(SELECT 1 FROM contacts WHERE id=$1 AND elder_id=$2)`,
			*updates.PreferredContactID, userID,
		).Scan(&belongsToUser); err != nil || !belongsToUser {
			return nil, ErrInvalidVoicePreferences
		}
	}
	_, err := s.db.ExecContext(ctx, `
		INSERT INTO voice_preferences(
			user_id, reminder_style, speech_rate, preferred_contact_id, include_daily_schedule
		) VALUES($1, COALESCE($2, 'GENTLE'), COALESCE($3, 0.85), NULLIF($4, '')::uuid, COALESCE($5, TRUE))
		ON CONFLICT(user_id) DO UPDATE SET
			reminder_style=COALESCE($2, voice_preferences.reminder_style),
			speech_rate=COALESCE($3, voice_preferences.speech_rate),
			preferred_contact_id=CASE WHEN $4 IS NULL THEN voice_preferences.preferred_contact_id ELSE NULLIF($4, '')::uuid END,
			include_daily_schedule=COALESCE($5, voice_preferences.include_daily_schedule),
			updated_at=NOW()`,
		userID, updates.ReminderStyle, updates.SpeechRate, updates.PreferredContactID, updates.IncludeDailySchedule,
	)
	if err != nil {
		return nil, fmt.Errorf("update voice preferences: %w", err)
	}
	return s.GetVoicePreferences(ctx, userID)
}

func (s *Service) GetEldersForCaregiver(ctx context.Context, caregiverID string) ([]Profile, error) {
	rows, err := s.db.QueryContext(ctx, `
		SELECT elder_id::text FROM caregiver_elder_links
		WHERE caregiver_id=$1 ORDER BY created_at`, caregiverID)
	if err != nil {
		return nil, fmt.Errorf("list elders: %w", err)
	}
	defer rows.Close()
	// A caregiver without a linked elder is a normal onboarding state. Return
	// an empty JSON array rather than null so Android can deserialize it as a
	// non-null List<UserDto>.
	profiles := make([]Profile, 0)
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

func (s *Service) CreateCaregiverLinkCode(ctx context.Context, elderID string) (*CaregiverLinkCode, error) {
	if allowed, err := s.userHasRole(ctx, elderID, "ELDER"); err != nil {
		return nil, err
	} else if !allowed {
		return nil, ErrRoleNotAllowed
	}
	code, err := newLinkCode()
	if err != nil {
		return nil, fmt.Errorf("generate caregiver link code: %w", err)
	}
	expiresAt := time.Now().UTC().Add(linkCodeLifetime)
	_, err = s.db.ExecContext(ctx, `
		INSERT INTO caregiver_link_codes(elder_id, code_hash, expires_at)
		VALUES($1,$2,$3)
		ON CONFLICT(elder_id) DO UPDATE SET
			code_hash=EXCLUDED.code_hash,
			expires_at=EXCLUDED.expires_at,
			created_at=NOW()`,
		elderID, hashLinkCode(code), expiresAt,
	)
	if err != nil {
		return nil, fmt.Errorf("store caregiver link code: %w", err)
	}
	return &CaregiverLinkCode{Code: formatLinkCode(code), ExpiresAt: expiresAt}, nil
}

func (s *Service) LinkElder(ctx context.Context, caregiverID, code string) (*Profile, error) {
	normalized := normalizeLinkCode(code)
	if len(normalized) != linkCodeLength {
		return nil, ErrInvalidLinkCode
	}
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return nil, err
	}
	defer tx.Rollback()

	var caregiverRole string
	if err := tx.QueryRowContext(ctx, `SELECT role FROM users WHERE id=$1`, caregiverID).Scan(&caregiverRole); err != nil {
		return nil, fmt.Errorf("read caregiver role: %w", err)
	}
	if caregiverRole != "CAREGIVER" {
		return nil, ErrRoleNotAllowed
	}

	var elderID string
	err = tx.QueryRowContext(ctx, `
		SELECT code.elder_id::text
		FROM caregiver_link_codes code
		JOIN users elder ON elder.id=code.elder_id AND elder.role='ELDER'
		WHERE code.code_hash=$1 AND code.expires_at > NOW()
		FOR UPDATE OF code`, hashLinkCode(normalized),
	).Scan(&elderID)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrInvalidLinkCode
	}
	if err != nil {
		return nil, fmt.Errorf("read caregiver link code: %w", err)
	}
	if _, err := tx.ExecContext(ctx, `
		INSERT INTO caregiver_elder_links(caregiver_id, elder_id)
		VALUES($1,$2)
		ON CONFLICT(caregiver_id, elder_id) DO NOTHING`, caregiverID, elderID); err != nil {
		return nil, fmt.Errorf("link caregiver to elder: %w", err)
	}
	if _, err := tx.ExecContext(ctx, `DELETE FROM caregiver_link_codes WHERE elder_id=$1`, elderID); err != nil {
		return nil, fmt.Errorf("consume caregiver link code: %w", err)
	}
	if err := tx.Commit(); err != nil {
		return nil, err
	}
	return s.GetByID(ctx, elderID)
}

func (s *Service) UnlinkElder(ctx context.Context, caregiverID, elderID string) error {
	result, err := s.db.ExecContext(ctx, `
		DELETE FROM caregiver_elder_links WHERE caregiver_id=$1 AND elder_id=$2`,
		caregiverID, elderID,
	)
	if err != nil {
		return fmt.Errorf("unlink elder: %w", err)
	}
	count, err := result.RowsAffected()
	if err != nil {
		return err
	}
	if count == 0 {
		return ErrLinkNotFound
	}
	return nil
}

func (s *Service) userHasRole(ctx context.Context, userID, role string) (bool, error) {
	var allowed bool
	err := s.db.QueryRowContext(ctx, `SELECT EXISTS(SELECT 1 FROM users WHERE id=$1 AND role=$2)`, userID, role).Scan(&allowed)
	return allowed, err
}

func newLinkCode() (string, error) {
	random := make([]byte, linkCodeLength)
	if _, err := rand.Read(random); err != nil {
		return "", err
	}
	code := make([]byte, linkCodeLength)
	for index, value := range random {
		code[index] = linkCodeAlphabet[int(value)%len(linkCodeAlphabet)]
	}
	return string(code), nil
}

func normalizeLinkCode(code string) string {
	return strings.ToUpper(strings.ReplaceAll(strings.TrimSpace(code), "-", ""))
}

func formatLinkCode(code string) string {
	normalized := normalizeLinkCode(code)
	if len(normalized) != linkCodeLength {
		return normalized
	}
	return normalized[:4] + "-" + normalized[4:]
}

func hashLinkCode(code string) string {
	sum := sha256.Sum256([]byte(normalizeLinkCode(code)))
	return hex.EncodeToString(sum[:])
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
