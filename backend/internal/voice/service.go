package voice

import (
	"bytes"
	"context"
	"crypto/rand"
	"crypto/sha256"
	"database/sql"
	"encoding/base64"
	"encoding/binary"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/dearly/backend/pkg/appclock"
	"github.com/dearly/backend/pkg/events"
)

var (
	ErrInvalidIntent           = errors.New("intent is not protected by speaker verification")
	ErrInvalidGrant            = errors.New("voice verification grant is invalid or expired")
	ErrVerificationRateLimited = errors.New("too many failed voice verification attempts")
	ErrVoiceReplayDetected     = errors.New("this recording was already used for verification")
)

type AIServiceError struct {
	StatusCode int
	Message    string
}

func (e *AIServiceError) Error() string {
	return fmt.Sprintf("AI service returned %d: %s", e.StatusCode, e.Message)
}

const (
	IntentCallContact      = "CALL_CONTACT"
	IntentMarkTaken        = "MARK_TAKEN"
	IntentUpdateSetting    = "UPDATE_SETTINGS"
	grantLifetime          = 2 * time.Minute
	verificationWindow     = 15 * time.Minute
	replayWindow           = 24 * time.Hour
	maxFailedVerifications = 5
)

var protectedIntents = map[string]struct{}{
	IntentCallContact:   {},
	IntentMarkTaken:     {},
	IntentUpdateSetting: {},
}

type VerificationResult struct {
	Passed            bool    `json:"passed"`
	Score             float64 `json:"score"`
	VerificationGrant string  `json:"verification_grant,omitempty"`
	ExpiresIn         int64   `json:"expires_in,omitempty"`
}

type VerificationAuditEntry struct {
	Intent    string    `json:"intent"`
	Outcome   string    `json:"outcome"`
	Score     *float64  `json:"score,omitempty"`
	CreatedAt time.Time `json:"created_at"`
}

// VoicePersonalization is returned only after SID has been constrained to the
// signed-in user's caregiver/elder group and re-authorized by PostgreSQL.
type VoicePersonalization struct {
	ReminderStyle        string  `json:"reminder_style"`
	SpeechRate           float64 `json:"speech_rate"`
	PreferredContactName *string `json:"preferred_contact_name,omitempty"`
	IncludeDailySchedule bool    `json:"include_daily_schedule"`
}

type enrolledSpeaker struct {
	UserID    string    `json:"user_id"`
	Embedding []float32 `json:"embedding"`
}

const enrolledSpeakersQuery = `
	WITH recognition_group AS (
		SELECT $1::uuid AS user_id
		UNION
		SELECT elder_id FROM caregiver_elder_links WHERE caregiver_id=$1
		UNION
		SELECT caregiver_id FROM caregiver_elder_links WHERE elder_id=$1
	)
	SELECT user_id::text, embedding_vector
	FROM voice_enrollments
	WHERE phrase_index=-1
	  AND user_id IN (SELECT user_id FROM recognition_group)`

const recognizedSpeakerNameQuery = `
	WITH recognition_group AS (
		SELECT $2::uuid AS user_id
		UNION
		SELECT elder_id FROM caregiver_elder_links WHERE caregiver_id=$2
		UNION
		SELECT caregiver_id FROM caregiver_elder_links WHERE elder_id=$2
	)
	SELECT name
	FROM users
	WHERE id=$1
	  AND id IN (SELECT user_id FROM recognition_group)`

type scheduledMedication struct {
	name  string
	slots []string
}

type Service struct {
	db           *sql.DB
	aiServiceURL string
	httpClient   *http.Client
	events       events.Publisher
	location     *time.Location
}

func NewService(db *sql.DB, aiServiceURL string, publisher events.Publisher) *Service {
	return &Service{
		db: db, aiServiceURL: strings.TrimRight(aiServiceURL, "/"),
		httpClient: &http.Client{Timeout: 90 * time.Second},
		events:     publisher,
		location:   appclock.LocationFromEnv(),
	}
}

func (s *Service) EnrollPhrase(ctx context.Context, userID string, phraseIndex int, filename string, audioBytes []byte) (int, error) {
	if phraseIndex < 0 || phraseIndex > 4 {
		return 0, errors.New("phrase_index must be between 0 and 4")
	}
	fields := map[string]string{"phrase_index": strconv.Itoa(phraseIndex)}
	response, err := s.multipart(ctx, "/enroll/", filename, audioBytes, fields, nil)
	if err != nil {
		return 0, err
	}
	var result struct {
		Embedding []float32 `json:"embedding"`
	}
	if err := json.Unmarshal(response, &result); err != nil || len(result.Embedding) == 0 {
		return 0, errors.New("AI enrollment response did not contain an embedding")
	}
	serialized := encodeVector(result.Embedding)
	_, err = s.db.ExecContext(ctx, `
		INSERT INTO voice_enrollments(user_id, phrase_index, embedding_vector)
		VALUES($1,$2,$3)
		ON CONFLICT(user_id, phrase_index) DO UPDATE SET embedding_vector=$3, created_at=NOW()`,
		userID, phraseIndex, serialized)
	if err != nil {
		return 0, err
	}
	var count int
	if err := s.db.QueryRowContext(ctx, `
		SELECT COUNT(*) FROM voice_enrollments WHERE user_id=$1 AND phrase_index BETWEEN 0 AND 4`,
		userID).Scan(&count); err != nil {
		return 0, err
	}
	if count == 5 {
		if err := s.ComputeAverageEmbedding(ctx, userID); err != nil {
			return 0, err
		}
	}
	_ = s.events.Publish(ctx, "voice.phrase.enrolled", userID, map[string]any{"phrase_index": phraseIndex, "enrolled_count": count})
	return count, nil
}

func (s *Service) ComputeAverageEmbedding(ctx context.Context, userID string) error {
	rows, err := s.db.QueryContext(ctx, `
		SELECT embedding_vector FROM voice_enrollments
		WHERE user_id=$1 AND phrase_index BETWEEN 0 AND 4 ORDER BY phrase_index`, userID)
	if err != nil {
		return err
	}
	defer rows.Close()
	var vectors [][]float32
	for rows.Next() {
		var encoded []byte
		if err := rows.Scan(&encoded); err != nil {
			return err
		}
		vector, err := decodeVector(encoded)
		if err != nil {
			return err
		}
		vectors = append(vectors, vector)
	}
	if len(vectors) != 5 {
		return errors.New("five enrollment phrases are required")
	}
	dimension := len(vectors[0])
	average := make([]float32, dimension)
	for _, vector := range vectors {
		if len(vector) != dimension {
			return errors.New("embedding dimensions do not match")
		}
		for index, value := range vector {
			average[index] += value / float32(len(vectors))
		}
	}
	_, err = s.db.ExecContext(ctx, `
		INSERT INTO voice_enrollments(user_id, phrase_index, embedding_vector)
		VALUES($1,-1,$2)
		ON CONFLICT(user_id, phrase_index) DO UPDATE SET embedding_vector=$2, created_at=NOW()`,
		userID, encodeVector(average))
	return err
}

func (s *Service) Verify(ctx context.Context, userID, intent, filename string, audioBytes []byte) (*VerificationResult, error) {
	intent = normalizeIntent(intent)
	if _, allowed := protectedIntents[intent]; !allowed {
		return nil, ErrInvalidIntent
	}
	audioHash := hashAudio(audioBytes)
	if err := s.checkVerificationProtection(ctx, userID, intent, audioHash); err != nil {
		return nil, err
	}
	var encoded []byte
	if err := s.db.QueryRowContext(ctx, `
		SELECT embedding_vector FROM voice_enrollments WHERE user_id=$1 AND phrase_index=-1`,
		userID).Scan(&encoded); err != nil {
		return nil, fmt.Errorf("load enrollment: %w", err)
	}
	vector, err := decodeVector(encoded)
	if err != nil {
		return nil, err
	}
	vectorJSON, _ := json.Marshal(vector)
	response, err := s.multipart(ctx, "/verify/", filename, audioBytes,
		map[string]string{"enrollment_embedding": string(vectorJSON)}, nil)
	if err != nil {
		return nil, err
	}
	var result struct {
		Passed bool    `json:"passed"`
		Score  float64 `json:"score"`
	}
	if err := json.Unmarshal(response, &result); err != nil {
		return nil, err
	}
	verification := &VerificationResult{Passed: result.Passed, Score: result.Score}
	outcome := "FAILED"
	if result.Passed {
		grant, err := s.issueVerificationGrant(ctx, userID, intent)
		if err != nil {
			return nil, err
		}
		verification.VerificationGrant = grant
		verification.ExpiresIn = int64(grantLifetime.Seconds())
		outcome = "PASSED"
	}
	if err := s.recordVerificationAttempt(ctx, userID, intent, outcome, &result.Score, audioHash); err != nil {
		return nil, err
	}
	_ = s.events.Publish(ctx, "voice.verification.completed", userID, map[string]any{
		"intent": intent, "passed": result.Passed, "score": result.Score,
	})
	return verification, nil
}

func (s *Service) VerificationAudit(ctx context.Context, userID string, limit int) ([]VerificationAuditEntry, error) {
	if limit < 1 || limit > 100 {
		limit = 20
	}
	rows, err := s.db.QueryContext(ctx, `
		SELECT intent, outcome, score, created_at
		FROM voice_verification_attempts
		WHERE user_id=$1
		ORDER BY created_at DESC
		LIMIT $2`, userID, limit)
	if err != nil {
		return nil, fmt.Errorf("load voice verification audit: %w", err)
	}
	defer rows.Close()
	entries := make([]VerificationAuditEntry, 0)
	for rows.Next() {
		var entry VerificationAuditEntry
		var score sql.NullFloat64
		if err := rows.Scan(&entry.Intent, &entry.Outcome, &score, &entry.CreatedAt); err != nil {
			return nil, err
		}
		if score.Valid {
			value := score.Float64
			entry.Score = &value
		}
		entries = append(entries, entry)
	}
	return entries, rows.Err()
}

func (s *Service) checkVerificationProtection(ctx context.Context, userID, intent, audioHash string) error {
	var failures int
	if err := s.db.QueryRowContext(ctx, `
		SELECT COUNT(*) FROM voice_verification_attempts
		WHERE user_id=$1 AND intent=$2 AND outcome IN ('FAILED','REPLAY_BLOCKED')
		  AND created_at > NOW()-($3 * INTERVAL '1 second')`,
		userID, intent, verificationWindow.Seconds(),
	).Scan(&failures); err != nil {
		return fmt.Errorf("check verification rate limit: %w", err)
	}
	if failures >= maxFailedVerifications {
		_ = s.recordVerificationAttempt(ctx, userID, intent, "RATE_LIMITED", nil, audioHash)
		return ErrVerificationRateLimited
	}
	var replayed bool
	if err := s.db.QueryRowContext(ctx, `
		SELECT EXISTS(
			SELECT 1 FROM voice_verification_attempts
			WHERE user_id=$1 AND intent=$2 AND audio_hash=$3 AND outcome='PASSED'
			  AND created_at > NOW()-($4 * INTERVAL '1 second')
		)`, userID, intent, audioHash, replayWindow.Seconds(),
	).Scan(&replayed); err != nil {
		return fmt.Errorf("check verification replay: %w", err)
	}
	if replayed {
		_ = s.recordVerificationAttempt(ctx, userID, intent, "REPLAY_BLOCKED", nil, audioHash)
		return ErrVoiceReplayDetected
	}
	return nil
}

func (s *Service) recordVerificationAttempt(
	ctx context.Context, userID, intent, outcome string, score *float64, audioHash string,
) error {
	_, err := s.db.ExecContext(ctx, `
		INSERT INTO voice_verification_attempts(user_id, intent, outcome, score, audio_hash)
		VALUES($1,$2,$3,$4,$5)`, userID, intent, outcome, score, audioHash)
	if err != nil {
		return fmt.Errorf("record voice verification attempt: %w", err)
	}
	return nil
}

func (s *Service) Query(ctx context.Context, userID, filename string, audioBytes []byte) (map[string]interface{}, error) {
	speakers, err := s.enrolledSpeakers(ctx, userID)
	if err != nil {
		return nil, err
	}
	serializedSpeakers, err := json.Marshal(speakers)
	if err != nil {
		return nil, err
	}
	response, err := s.multipart(ctx, "/query/", filename, audioBytes,
		map[string]string{"enrolled_speakers": string(serializedSpeakers)},
		map[string]string{"X-User-ID": userID})
	if err != nil {
		return nil, err
	}
	var result map[string]interface{}
	if err := json.Unmarshal(response, &result); err != nil {
		return nil, err
	}
	if err := s.enrichQueryResponse(ctx, userID, result); err != nil {
		return nil, err
	}
	_ = s.events.Publish(ctx, "voice.query.completed", userID, map[string]any{
		"intent": result["intent"], "sv_required": result["sv_required"], "sv_passed": result["sv_passed"],
	})
	return result, nil
}

// PublicQuery is intentionally limited to information that is safe before a
// user signs in. It never loads a profile, enrollment, medication, or contact.
func (s *Service) PublicQuery(ctx context.Context, filename string, audioBytes []byte) (map[string]interface{}, error) {
	response, err := s.multipart(ctx, "/query/", filename, audioBytes, nil, nil)
	if err != nil {
		return nil, err
	}
	var result map[string]interface{}
	if err := json.Unmarshal(response, &result); err != nil {
		return nil, err
	}
	intent, _ := result["intent"].(string)
	result["intent"] = normalizeIntent(intent)
	result["sv_required"] = false
	result["sv_passed"] = nil
	result["identified_user_id"] = nil
	result["identification_score"] = 0.0
	switch normalizeIntent(intent) {
	case "ASK_TIME":
		result["response_text"] = vietnameseTimeResponse(time.Now().In(s.location))
	case "ASK_DATE":
		result["response_text"] = vietnameseDateResponse(time.Now().In(s.location))
	case "GREETING":
		result["response_text"] = "Chào bác. Dearly có thể cho bác biết giờ hoặc ngày hôm nay."
	default:
		result["intent"] = "PUBLIC_HELP"
		result["response_text"] = "Bác có thể hỏi Dearly bây giờ là mấy giờ hoặc hôm nay là ngày mấy nhé."
	}
	return result, nil
}

func (s *Service) ConsumeVerificationGrant(ctx context.Context, userID, intent, grant string) error {
	intent = normalizeIntent(intent)
	if _, allowed := protectedIntents[intent]; !allowed || strings.TrimSpace(grant) == "" {
		return ErrInvalidGrant
	}
	var consumed string
	err := s.db.QueryRowContext(ctx, `
		DELETE FROM voice_verification_grants
		WHERE token_hash=$1 AND user_id=$2 AND intent=$3 AND expires_at > NOW()
		RETURNING token_hash`, hashGrant(grant), userID, intent,
	).Scan(&consumed)
	if errors.Is(err, sql.ErrNoRows) {
		return ErrInvalidGrant
	}
	if err != nil {
		return fmt.Errorf("consume verification grant: %w", err)
	}
	return nil
}

func (s *Service) issueVerificationGrant(ctx context.Context, userID, intent string) (string, error) {
	random := make([]byte, 32)
	if _, err := rand.Read(random); err != nil {
		return "", fmt.Errorf("generate verification grant: %w", err)
	}
	grant := base64.RawURLEncoding.EncodeToString(random)
	_, err := s.db.ExecContext(ctx, `
		INSERT INTO voice_verification_grants(token_hash, user_id, intent, expires_at)
		VALUES($1,$2,$3,$4)`, hashGrant(grant), userID, intent, time.Now().UTC().Add(grantLifetime))
	if err != nil {
		return "", fmt.Errorf("store verification grant: %w", err)
	}
	return grant, nil
}

func (s *Service) enrolledSpeakers(ctx context.Context, userID string) ([]enrolledSpeaker, error) {
	rows, err := s.db.QueryContext(ctx, enrolledSpeakersQuery, userID)
	if err != nil {
		return nil, fmt.Errorf("load enrolled speakers: %w", err)
	}
	defer rows.Close()
	speakers := make([]enrolledSpeaker, 0)
	for rows.Next() {
		var userID string
		var encoded []byte
		if err := rows.Scan(&userID, &encoded); err != nil {
			return nil, err
		}
		embedding, err := decodeVector(encoded)
		if err != nil {
			return nil, err
		}
		speakers = append(speakers, enrolledSpeaker{UserID: userID, Embedding: embedding})
	}
	return speakers, rows.Err()
}

func (s *Service) enrichQueryResponse(ctx context.Context, userID string, result map[string]interface{}) error {
	intent, _ := result["intent"].(string)
	identifiedUserID, _ := result["identified_user_id"].(string)
	recognizedName := ""
	medicationOwnerID := userID
	var personalization *VoicePersonalization
	if identifiedUserID != "" {
		name, err := s.recognizedSpeakerName(ctx, identifiedUserID, userID)
		if err != nil {
			if !errors.Is(err, sql.ErrNoRows) {
				return err
			}
		} else {
			recognizedName = name
			medicationOwnerID = identifiedUserID
			result["recognized_user_name"] = name
			preferences, preferenceErr := s.voicePersonalization(ctx, identifiedUserID)
			if preferenceErr != nil {
				return preferenceErr
			}
			personalization = preferences
			result["personalization"] = preferences
		}
	}
	switch normalizeIntent(intent) {
	case "ASK_TIME":
		result["response_text"] = vietnameseTimeResponse(time.Now().In(s.location))
	case "ASK_DATE":
		result["response_text"] = vietnameseDateResponse(time.Now().In(s.location))
	case "CHECK_MEDICATIONS":
		response, err := s.medicationScheduleResponse(ctx, medicationOwnerID)
		if err != nil {
			return err
		}
		if personalization != nil {
			response = personalizedScheduleResponse(response, personalization)
		}
		result["response_text"] = response
	}

	if response, ok := result["response_text"].(string); ok && strings.TrimSpace(response) != "" && recognizedName != "" {
		result["response_text"] = personalizedGreeting(recognizedName, response, personalization)
	}
	return nil
}

func (s *Service) voicePersonalization(ctx context.Context, userID string) (*VoicePersonalization, error) {
	preferences := &VoicePersonalization{ReminderStyle: "GENTLE", SpeechRate: 0.85, IncludeDailySchedule: true}
	err := s.db.QueryRowContext(ctx, `
		SELECT p.reminder_style, p.speech_rate, COALESCE(c.nickname, c.full_name),
		       p.include_daily_schedule
		FROM voice_preferences p
		LEFT JOIN contacts c ON c.id=p.preferred_contact_id AND c.elder_id=p.user_id
		WHERE p.user_id=$1`, userID,
	).Scan(
		&preferences.ReminderStyle, &preferences.SpeechRate,
		&preferences.PreferredContactName, &preferences.IncludeDailySchedule,
	)
	if errors.Is(err, sql.ErrNoRows) {
		return preferences, nil
	}
	if err != nil {
		return nil, fmt.Errorf("load voice personalization: %w", err)
	}
	return preferences, nil
}

func personalizedGreeting(name, response string, preferences *VoicePersonalization) string {
	if preferences != nil && preferences.ReminderStyle == "DIRECT" {
		return fmt.Sprintf("%s, %s", name, response)
	}
	return fmt.Sprintf("Chào %s. %s", name, response)
}

func personalizedScheduleResponse(response string, preferences *VoicePersonalization) string {
	if !preferences.IncludeDailySchedule {
		return response
	}
	prefix := "Nhắc nhẹ: "
	if preferences.ReminderStyle == "DIRECT" {
		prefix = "Lịch cần thực hiện: "
	}
	if preferences.PreferredContactName == nil || strings.TrimSpace(*preferences.PreferredContactName) == "" {
		return prefix + response
	}
	return fmt.Sprintf("%s%s Khi cần hỗ trợ, Dearly sẽ ưu tiên liên hệ %s.",
		prefix, response, *preferences.PreferredContactName)
}

func (s *Service) recognizedSpeakerName(ctx context.Context, identifiedUserID, requestUserID string) (string, error) {
	var name string
	if err := s.db.QueryRowContext(ctx, recognizedSpeakerNameQuery, identifiedUserID, requestUserID).Scan(&name); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return "", err
		}
		return "", fmt.Errorf("load identified speaker: %w", err)
	}
	name = strings.TrimSpace(name)
	if name == "" {
		return "", sql.ErrNoRows
	}
	return name, nil
}

func (s *Service) medicationScheduleResponse(ctx context.Context, elderID string) (string, error) {
	rows, err := s.db.QueryContext(ctx, `
		SELECT name, time_slots
		FROM medications
		WHERE elder_id=$1
		ORDER BY name`, elderID)
	if err != nil {
		return "", fmt.Errorf("load medication schedule: %w", err)
	}
	defer rows.Close()

	medications := make([]scheduledMedication, 0)
	for rows.Next() {
		var name string
		var slotsJSON []byte
		if err := rows.Scan(&name, &slotsJSON); err != nil {
			return "", err
		}
		var slots []string
		if err := json.Unmarshal(slotsJSON, &slots); err != nil {
			return "", fmt.Errorf("decode medication schedule: %w", err)
		}
		medications = append(medications, scheduledMedication{name: name, slots: slots})
	}
	if err := rows.Err(); err != nil {
		return "", err
	}
	return vietnameseMedicationScheduleResponse(medications), nil
}

func vietnameseMedicationScheduleResponse(medications []scheduledMedication) string {
	if len(medications) == 0 {
		return "Hôm nay bác chưa có thuốc nào được cài đặt."
	}
	items := make([]string, 0, len(medications))
	for _, medication := range medications {
		if len(medication.slots) == 0 {
			items = append(items, medication.name)
			continue
		}
		items = append(items, fmt.Sprintf("%s lúc %s", medication.name, strings.Join(medication.slots, ", ")))
	}
	return "Lịch thuốc hôm nay của bác: " + strings.Join(items, "; ") + "."
}

func vietnameseTimeResponse(now time.Time) string {
	return fmt.Sprintf("Bây giờ là %d giờ %02d phút.", now.Hour(), now.Minute())
}

func vietnameseDateResponse(now time.Time) string {
	weekdays := [...]string{"Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy"}
	return fmt.Sprintf("Hôm nay là %s, ngày %d tháng %d năm %d.", weekdays[now.Weekday()], now.Day(), now.Month(), now.Year())
}

func normalizeIntent(intent string) string {
	return strings.ToUpper(strings.TrimSpace(intent))
}

func hashGrant(grant string) string {
	sum := sha256.Sum256([]byte(strings.TrimSpace(grant)))
	return hex.EncodeToString(sum[:])
}

func hashAudio(audio []byte) string {
	sum := sha256.Sum256(audio)
	return hex.EncodeToString(sum[:])
}

func (s *Service) Reset(ctx context.Context, userID string) error {
	_, err := s.db.ExecContext(ctx, `DELETE FROM voice_enrollments WHERE user_id=$1`, userID)
	return err
}

func (s *Service) multipart(ctx context.Context, path, filename string, audio []byte, fields, headers map[string]string) ([]byte, error) {
	var body bytes.Buffer
	writer := multipart.NewWriter(&body)
	part, err := writer.CreateFormFile("audio", filename)
	if err != nil {
		return nil, err
	}
	if _, err := part.Write(audio); err != nil {
		return nil, err
	}
	for key, value := range fields {
		if err := writer.WriteField(key, value); err != nil {
			return nil, err
		}
	}
	if err := writer.Close(); err != nil {
		return nil, err
	}
	request, err := http.NewRequestWithContext(ctx, http.MethodPost, s.aiServiceURL+path, &body)
	if err != nil {
		return nil, err
	}
	request.Header.Set("Content-Type", writer.FormDataContentType())
	for key, value := range headers {
		request.Header.Set(key, value)
	}
	response, err := s.httpClient.Do(request)
	if err != nil {
		return nil, fmt.Errorf("AI service request: %w", err)
	}
	defer response.Body.Close()
	responseBody, err := io.ReadAll(io.LimitReader(response.Body, 10<<20))
	if err != nil {
		return nil, err
	}
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return nil, &AIServiceError{StatusCode: response.StatusCode, Message: string(responseBody)}
	}
	return responseBody, nil
}

func encodeVector(vector []float32) []byte {
	buffer := bytes.NewBuffer(make([]byte, 0, len(vector)*4))
	for _, value := range vector {
		_ = binary.Write(buffer, binary.LittleEndian, value)
	}
	return buffer.Bytes()
}

func decodeVector(encoded []byte) ([]float32, error) {
	if len(encoded)%4 != 0 {
		return nil, errors.New("invalid embedding bytes")
	}
	vector := make([]float32, len(encoded)/4)
	err := binary.Read(bytes.NewReader(encoded), binary.LittleEndian, &vector)
	return vector, err
}
