package user

// Service contains business logic for user profiles.
//
// TODO(W2-W3): Implement all methods below.
type Service struct {
	// TODO(W2): inject db *sql.DB (or UserRepository interface)
}

func NewService() *Service { return &Service{} }

// GetByID fetches a user by their UUID.
//
// TODO(W2):
//  1. SELECT * FROM users WHERE id = $1
//  2. If role = ELDER: also SELECT * FROM elder_profiles WHERE user_id = $1
//  3. Compute health_status_auto:
//       COUNT(*) FROM medication_logs WHERE medication_id IN
//         (SELECT id FROM medications WHERE elder_id = $1)
//         AND scheduled_time::date = CURRENT_DATE
//         AND status = 'PENDING'
//       NORMAL if count <= 1, WARNING if 2–4, CRITICAL if >= 5
//  4. Set health_status_effective = COALESCE(override, auto)
func (s *Service) GetByID(userID string) (interface{}, error) {
	// TODO(W2): implement
	return nil, nil
}

// Update applies partial updates to a user's profile fields.
//
// TODO(W2):
//  1. Build dynamic UPDATE query for non-nil fields only
//  2. UPDATE users SET name=$1, city=$2, avatar_url=$3, fcm_token=$4,
//     updated_at=NOW() WHERE id=$5
//  3. Return updated user record
func (s *Service) Update(userID string, updates map[string]interface{}) (interface{}, error) {
	// TODO(W2): implement
	return nil, nil
}

// GetEldersForCaregiver returns all elders linked to a caregiver.
//
// TODO(W3):
//  1. Verify requesting user is the caregiver (authorization check)
//  2. JOIN users + caregiver_elder_links ON elder_id
//  3. Compute health_status_effective for each elder (see GetByID above)
func (s *Service) GetEldersForCaregiver(caregiverID string) ([]interface{}, error) {
	// TODO(W3): implement
	return nil, nil
}
