package contact

// Service contains business logic for the call-list / contact module.
//
// TODO(W3): Implement all methods.
type Service struct {
	// TODO(W3): inject db *sql.DB
}

func NewService() *Service { return &Service{} }

// List returns all contacts for an elder, sorted by nickname.
//
// TODO(W3):
//  1. SELECT * FROM contacts WHERE elder_id = $1 ORDER BY nickname ASC
func (s *Service) List(elderID string) ([]interface{}, error) {
	return nil, nil
}

// Create inserts a new contact row.
//
// TODO(W3):
//  1. Validate phone_number format
//  2. Validate call_method in ('PHONE', 'ZALO_VIDEO')
//  3. INSERT INTO contacts (elder_id, nickname, full_name, phone_number, relationship, call_method)
//     VALUES (...) RETURNING *
func (s *Service) Create(elderID string, data map[string]interface{}) (interface{}, error) {
	return nil, nil
}

// Update applies changes to an existing contact.
//
// TODO(W3):
//  1. SELECT elder_id FROM contacts WHERE id = $1 — verify ownership
//  2. UPDATE contacts SET ... WHERE id = $1
func (s *Service) Update(contactID, elderID string, data map[string]interface{}) (interface{}, error) {
	return nil, nil
}

// Delete removes a contact after verifying ownership.
//
// TODO(W3):
//  1. DELETE FROM contacts WHERE id = $1 AND elder_id = $2
//  2. Return error if no rows deleted (not found / not owned)
func (s *Service) Delete(contactID, elderID string) error {
	return nil
}
