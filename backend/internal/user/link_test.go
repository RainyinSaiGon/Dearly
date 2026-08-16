package user

import (
	"strings"
	"testing"
)

func TestNewLinkCodeUsesExpectedAlphabet(t *testing.T) {
	code, err := newLinkCode()
	if err != nil {
		t.Fatal(err)
	}
	if len(code) != linkCodeLength {
		t.Fatalf("expected %d characters, got %q", linkCodeLength, code)
	}
	for _, character := range code {
		if !strings.ContainsRune(linkCodeAlphabet, character) {
			t.Fatalf("unexpected character %q in code %q", character, code)
		}
	}
}

func TestLinkCodeNormalizationAndHashing(t *testing.T) {
	formatted := formatLinkCode("abcd2345")
	if formatted != "ABCD-2345" {
		t.Fatalf("unexpected formatted code: %s", formatted)
	}
	if hashLinkCode("ABCD-2345") != hashLinkCode(" abcd2345 ") {
		t.Fatal("equivalent codes must produce the same hash")
	}
}
