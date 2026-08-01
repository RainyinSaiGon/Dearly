package medication

// Service contains business logic for medication schedules and dose logging.
//
// TODO(W3): Implement all methods below.
type Service struct {
	// TODO(W3): inject db *sql.DB, notificationService *notification.Service
}

func NewService() *Service { return &Service{} }

// List returns medications with today's dose log status for an elder.
//
// TODO(W3): Run the JOIN query from handler.go comment.
// Aggregate results into a struct like:
//
//	type MedicationWithLogs struct {
//	    ID               string
//	    Name             string
//	    FrequencyPerDay  int
//	    TimeSlots        []string          // ["07:00","13:00"]
//	    TodayLogs        []DoseLog         // per slot: status + taken_at
//	}
func (s *Service) List(elderID string) ([]interface{}, error) {
	return nil, nil
}

// Create inserts a medication and schedules FCM reminders.
//
// TODO(W3):
//  1. INSERT INTO medications ... RETURNING id
//  2. For each time_slot, compute next scheduled_time (today if not past, tomorrow otherwise)
//  3. Call notificationService.ScheduleMedicationReminder(elderID, medID, scheduledTime)
func (s *Service) Create(elderID string, data map[string]interface{}) (interface{}, error) {
	return nil, nil
}

// MarkTaken upserts a medication_log row with status=TAKEN.
//
// TODO(W3):
//  1. INSERT INTO medication_logs (medication_id, scheduled_time, taken_at, status)
//     VALUES ($1, $2, NOW(), 'TAKEN')
//     ON CONFLICT (medication_id, scheduled_time) DO UPDATE
//     SET status='TAKEN', taken_at=NOW()
//  2. Notify caregiver via FCM: "[Elder name] đã uống [med name]"
func (s *Service) MarkTaken(medicationID, scheduledTime string) error {
	return nil
}

// SnoozeReminder updates a dose log to SNOOZED for 10 minutes.
// Called when elder taps "Snooze 10 min" on Android notification shade.
//
// TODO(W3):
//  1. UPDATE medication_logs SET status='SNOOZED', snoozed_until=NOW()+interval'10 min'
//     WHERE medication_id=$1 AND scheduled_time=$2
//  2. Re-schedule a new FCM notification for snoozed_until time
func (s *Service) SnoozeReminder(medicationID, scheduledTime string) error {
	return nil
}
