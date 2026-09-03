package auth

import (
	"strings"
	"testing"
)

func TestUpsertUserConflictClauseMatchesPartialFirebaseUIDIndex(t *testing.T) {
	const conflictClause = "ON CONFLICT(firebase_uid) WHERE firebase_uid IS NOT NULL DO UPDATE"
	if !strings.Contains(upsertUserQuery, conflictClause) {
		t.Fatalf("user upsert must match the partial firebase_uid index with %q", conflictClause)
	}
}
