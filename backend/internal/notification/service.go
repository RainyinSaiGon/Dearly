package notification

// Service sends push notifications via Firebase Cloud Messaging (FCM).
//
// TODO(W3): Implement all methods below.
// Reference: https://firebase.google.com/docs/cloud-messaging/send-message
type Service struct {
	// TODO(W3): inject *messaging.Client from Firebase Admin SDK
}

func NewService() *Service { return &Service{} }

// SendMedicationReminder pushes a medication reminder to the elder's device.
// The notification includes "Taken" and "Snooze 10 min" action buttons.
//
// TODO(W3):
//  1. Fetch fcm_token from users WHERE id = elderID
//  2. Build an FCM message with:
//       Title: "💊 Đến giờ uống thuốc"
//       Body:  "[Med name] — [HH:MM]"
//       Data:  { medication_id, scheduled_time, action: "MEDICATION_REMINDER" }
//       Android notification actions:
//         - "Đã uống" → POST /api/v1/medications/:id/taken (via WorkManager on device)
//         - "Nhắc sau 10 phút" → POST /api/v1/medications/:id/snooze
//  3. messaging.Client.Send(ctx, message)
//  4. Log the FCM message ID for debugging
func (s *Service) SendMedicationReminder(elderID, medicationID, medName, scheduledTime string) error {
	// TODO(W3): implement
	return nil
}

// SendToCaregiver pushes a status update to a caregiver (e.g., elder took medication).
//
// TODO(W3):
//  1. Look up caregiver_id from caregiver_elder_links WHERE elder_id = $1
//  2. Fetch caregiver's fcm_token
//  3. Send notification: "[Elder name] đã [action]"
func (s *Service) SendToCaregiver(elderID, message string) error {
	// TODO(W3): implement
	return nil
}

// ScheduleMedicationReminder registers a timed job to fire SendMedicationReminder
// at each scheduled time slot (e.g., 07:00, 13:00, 19:00).
//
// TODO(W3):
//  Simple approach for demo: use a cron-style goroutine that checks
//  medication_logs every minute and fires FCM for PENDING doses whose
//  scheduled_time is within the next minute.
//
//  Production approach: use a task queue (e.g., Asynq + Redis).
func (s *Service) ScheduleMedicationReminder(elderID, medicationID, scheduledTime string) error {
	// TODO(W3): implement
	return nil
}
