package auth

import "testing"

func TestRoleSelectionRequiredIsDistinctFromInvalidRole(t *testing.T) {
	if ErrRoleSelectionRequired == ErrInvalidRole {
		t.Fatal("a new account must be distinguishable from an invalid role")
	}
}
