package appclock

import (
	"testing"
	"time"
)

func TestDayBoundsUsesConfiguredLocalDate(t *testing.T) {
	location, err := time.LoadLocation("Asia/Ho_Chi_Minh")
	if err != nil {
		t.Fatal(err)
	}
	at := time.Date(2026, time.August, 16, 18, 30, 0, 0, time.UTC)

	start, end := DayBounds(at, location)

	if got := start.Format(time.RFC3339); got != "2026-08-17T00:00:00+07:00" {
		t.Fatalf("unexpected start: %s", got)
	}
	if end.Sub(start) != 24*time.Hour {
		t.Fatalf("unexpected day length: %s", end.Sub(start))
	}
}

func TestLocationFromEnvFallsBackToUTC(t *testing.T) {
	t.Setenv("APP_TIMEZONE", "not/a-timezone")

	if got := LocationFromEnv(); got != time.UTC {
		t.Fatalf("expected UTC fallback, got %s", got)
	}
}
