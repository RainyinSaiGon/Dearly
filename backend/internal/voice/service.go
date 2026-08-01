package voice

import "net/http"

// Service handles the business logic for voice enrollment and verification.
// It acts as a proxy between the Go API and the Python ai-service.
//
// TODO(W4-W5): Implement all methods.
type Service struct {
	// TODO(W4): inject db *sql.DB, aiServiceURL string, httpClient *http.Client
	aiServiceURL string
	httpClient   *http.Client
}

func NewService(aiServiceURL string) *Service {
	return &Service{
		aiServiceURL: aiServiceURL,
		httpClient:   &http.Client{},
	}
}

// EnrollPhrase forwards one audio phrase to ai-service and stores the embedding.
//
// TODO(W4):
//  1. POST audio to aiServiceURL + "/enroll" (multipart form)
//  2. Parse embedding_vector ([]float32) from JSON response
//  3. Serialize embedding to []byte for BYTEA storage
//  4. UPSERT voice_enrollments (user_id, phrase_index, embedding_vector)
//  5. Return enrolled_count (total phrases recorded so far)
func (s *Service) EnrollPhrase(userID string, phraseIndex int, audioBytes []byte) (int, error) {
	// TODO(W4): implement
	return 0, nil
}

// ComputeAverageEmbedding averages all 5 phrase embeddings into a single vector.
// Should be called after phrase_index 4 is successfully enrolled.
//
// TODO(W4):
//  1. SELECT embedding_vector FROM voice_enrollments WHERE user_id=$1 ORDER BY phrase_index
//  2. Deserialize each BYTEA to []float32
//  3. Compute element-wise average across all 5 vectors
//  4. Store result in a "master" enrollment row (phrase_index = -1 or a separate column)
func (s *Service) ComputeAverageEmbedding(userID string) error {
	// TODO(W4): implement
	return nil
}

// Verify compares incoming audio against the user's stored average embedding.
// Threshold: cosine similarity >= 0.80 → pass.
//
// TODO(W5):
//  1. Load average embedding from voice_enrollments WHERE user_id=$1 AND phrase_index=-1
//  2. POST audio to aiServiceURL + "/verify" with the stored embedding
//  3. ai-service computes cosine similarity and returns { passed, score }
//  4. Log verification attempt (for audit; do NOT store audio)
func (s *Service) Verify(userID string, audioBytes []byte) (passed bool, score float64, err error) {
	// TODO(W5): implement
	return false, 0, nil
}

// Query forwards audio to the full voice assistant pipeline.
//
// TODO(W5):
//  1. POST audio to aiServiceURL + "/query" with userID header
//  2. Parse response: transcript, intent, response_text, response_audio_url, sv_required, sv_passed
//  3. If sv_required is true, the response from ai-service already handles SV internally
//  4. Store query log for caregiver activity view
func (s *Service) Query(userID string, audioBytes []byte) (map[string]interface{}, error) {
	// TODO(W5): implement
	return nil, nil
}
