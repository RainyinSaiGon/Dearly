package voice

import (
	"context"
	"errors"
	"strings"
	"testing"
	"time"
)

func TestVerifyRejectsUnprotectedIntentBeforeReadingEnrollment(t *testing.T) {
	service := &Service{}

	_, err := service.Verify(context.Background(), "user", "ASK_TIME", "voice.wav", []byte("audio"))

	if !errors.Is(err, ErrInvalidIntent) {
		t.Fatalf("expected ErrInvalidIntent, got %v", err)
	}
}

func TestGrantHashAndIntentNormalization(t *testing.T) {
	if normalizeIntent(" mark_taken ") != IntentMarkTaken {
		t.Fatal("expected normalized protected intent")
	}
	if hashGrant("grant") != hashGrant(" grant ") {
		t.Fatal("expected surrounding whitespace to be ignored")
	}
	if hashGrant("grant") == hashGrant("different") {
		t.Fatal("different grants must not share a hash")
	}
}

func TestAudioHashIsStableAndDistinct(t *testing.T) {
	if hashAudio([]byte("recording")) != hashAudio([]byte("recording")) {
		t.Fatal("the same recording must have the same replay-protection hash")
	}
	if hashAudio([]byte("recording")) == hashAudio([]byte("different")) {
		t.Fatal("different recordings must not share a replay-protection hash")
	}
}

func TestGeneralVoiceResponsesContainLiveValues(t *testing.T) {
	at := time.Date(2026, time.August, 30, 9, 5, 0, 0, time.UTC)

	if got, want := vietnameseTimeResponse(at), "Bây giờ là 9 giờ 05 phút."; got != want {
		t.Fatalf("unexpected time response: got %q, want %q", got, want)
	}
	if got, want := vietnameseDateResponse(at), "Hôm nay là Chủ nhật, ngày 30 tháng 8 năm 2026."; got != want {
		t.Fatalf("unexpected date response: got %q, want %q", got, want)
	}
}

func TestMedicationScheduleResponseUsesStoredSchedule(t *testing.T) {
	if got, want := vietnameseMedicationScheduleResponse(nil), "Hôm nay bác chưa có thuốc nào được cài đặt."; got != want {
		t.Fatalf("unexpected empty schedule response: got %q, want %q", got, want)
	}

	got := vietnameseMedicationScheduleResponse([]scheduledMedication{
		{name: "Vitamin D", slots: []string{"08:00", "20:00"}},
		{name: "Paracetamol"},
	})
	want := "Lịch thuốc hôm nay của bác: Vitamin D lúc 08:00, 20:00; Paracetamol."
	if got != want {
		t.Fatalf("unexpected schedule response: got %q, want %q", got, want)
	}
}

func TestPersonalizedScheduleUsesStyleAndPreferredContact(t *testing.T) {
	contact := "con Lan"
	response := personalizedScheduleResponse(
		"Lịch thuốc hôm nay của bác: Vitamin D lúc 08:00.",
		&VoicePersonalization{
			ReminderStyle: "GENTLE", PreferredContactName: &contact, IncludeDailySchedule: true,
		},
	)
	for _, expected := range []string{"Nhắc nhẹ:", "Vitamin D", "con Lan"} {
		if !strings.Contains(response, expected) {
			t.Fatalf("personalized schedule %q does not contain %q", response, expected)
		}
	}

	direct := personalizedScheduleResponse("Lịch thuốc.", &VoicePersonalization{ReminderStyle: "DIRECT", IncludeDailySchedule: true})
	if !strings.HasPrefix(direct, "Lịch cần thực hiện:") {
		t.Fatalf("expected direct reminder wording, got %q", direct)
	}
}

func TestEnrolledSpeakerQueryLimitsRecognitionToTrustedLinks(t *testing.T) {
	for _, query := range []string{enrolledSpeakersQuery, recognizedSpeakerNameQuery} {
		for _, clause := range []string{
			"WITH recognition_group AS",
			"SELECT elder_id FROM caregiver_elder_links WHERE caregiver_id=$",
			"SELECT caregiver_id FROM caregiver_elder_links WHERE elder_id=$",
			"id IN (SELECT user_id FROM recognition_group)",
		} {
			if !strings.Contains(query, clause) {
				t.Fatalf("recognition query is missing %q", clause)
			}
		}
	}
	if !strings.Contains(enrolledSpeakersQuery, "phrase_index=-1") {
		t.Fatal("speaker lookup must use only average enrollment profiles")
	}
}
