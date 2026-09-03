package user

import (
	"encoding/json"
	"testing"
)

func TestEmptyCaregiverElderListSerializesAsArray(t *testing.T) {
	profiles := make([]Profile, 0)
	payload, err := json.Marshal(profiles)
	if err != nil {
		t.Fatalf("marshal empty elder list: %v", err)
	}
	if got, want := string(payload), "[]"; got != want {
		t.Fatalf("empty elder list = %s, want %s", got, want)
	}
}
