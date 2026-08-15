package medication

import "testing"

func TestValidateMedicationInput(t *testing.T) {
	input := Input{
		Name: "Amlodipine", FrequencyPerDay: 2,
		TimeSlots: []string{"20:00", "08:00"},
	}
	if err := validate(&input); err != nil {
		t.Fatal(err)
	}
	if input.TimeSlots[0] != "08:00" {
		t.Fatalf("expected sorted time slots, got %#v", input.TimeSlots)
	}
}

func TestValidateMedicationInputRejectsMismatch(t *testing.T) {
	input := Input{Name: "Aspirin", FrequencyPerDay: 2, TimeSlots: []string{"08:00"}}
	if err := validate(&input); err == nil {
		t.Fatal("expected frequency mismatch error")
	}
}

func TestValidateMedicationInputRejectsBadTime(t *testing.T) {
	input := Input{Name: "Aspirin", FrequencyPerDay: 1, TimeSlots: []string{"8am"}}
	if err := validate(&input); err == nil {
		t.Fatal("expected invalid time error")
	}
}
