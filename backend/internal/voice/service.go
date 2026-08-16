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

	"github.com/dearly/backend/pkg/events"
)

var (
	ErrInvalidIntent = errors.New("intent is not protected by speaker verification")
	ErrInvalidGrant  = errors.New("voice verification grant is invalid or expired")
)

const (
	IntentCallContact   = "CALL_CONTACT"
	IntentMarkTaken     = "MARK_TAKEN"
	IntentUpdateSetting = "UPDATE_SETTINGS"
	grantLifetime       = 2 * time.Minute
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

type enrolledSpeaker struct {
	UserID    string    `json:"user_id"`
	Embedding []float32 `json:"embedding"`
}

type Service struct {
	db           *sql.DB
	aiServiceURL string
	httpClient   *http.Client
	events       events.Publisher
}

func NewService(db *sql.DB, aiServiceURL string, publisher events.Publisher) *Service {
	return &Service{
		db: db, aiServiceURL: strings.TrimRight(aiServiceURL, "/"),
		httpClient: &http.Client{Timeout: 90 * time.Second}, events: publisher,
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
	if result.Passed {
		grant, err := s.issueVerificationGrant(ctx, userID, intent)
		if err != nil {
			return nil, err
		}
		verification.VerificationGrant = grant
		verification.ExpiresIn = int64(grantLifetime.Seconds())
	}
	_ = s.events.Publish(ctx, "voice.verification.completed", userID, map[string]any{
		"intent": intent, "passed": result.Passed, "score": result.Score,
	})
	return verification, nil
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
	_ = s.events.Publish(ctx, "voice.query.completed", userID, map[string]any{
		"intent": result["intent"], "sv_required": result["sv_required"], "sv_passed": result["sv_passed"],
	})
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
	rows, err := s.db.QueryContext(ctx, `
		SELECT user_id::text, embedding_vector
		FROM voice_enrollments WHERE phrase_index=-1 AND user_id=$1`, userID)
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

func normalizeIntent(intent string) string {
	return strings.ToUpper(strings.TrimSpace(intent))
}

func hashGrant(grant string) string {
	sum := sha256.Sum256([]byte(strings.TrimSpace(grant)))
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
		return nil, fmt.Errorf("AI service returned %d: %s", response.StatusCode, string(responseBody))
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
