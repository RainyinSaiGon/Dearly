package appclock

import (
	"os"
	"time"
)

const defaultTimezone = "Asia/Ho_Chi_Minh"

func LocationFromEnv() *time.Location {
	name := os.Getenv("APP_TIMEZONE")
	if name == "" {
		name = defaultTimezone
	}
	location, err := time.LoadLocation(name)
	if err != nil {
		return time.UTC
	}
	return location
}

func DayBounds(at time.Time, location *time.Location) (time.Time, time.Time) {
	local := at.In(location)
	start := time.Date(local.Year(), local.Month(), local.Day(), 0, 0, 0, 0, location)
	return start, start.AddDate(0, 0, 1)
}
