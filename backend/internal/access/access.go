package access

import (
	"context"
	"database/sql"
	"errors"

	"github.com/dearly/backend/internal/auth"
	"github.com/gin-gonic/gin"
)

var ErrForbidden = errors.New("user cannot access the requested elder")

type Resolver struct {
	DB *sql.DB
}

func (r Resolver) Resolve(c *gin.Context, requestedElderID string) (string, error) {
	return ResolveElderID(c.Request.Context(), r.DB, auth.UserID(c), auth.Role(c), requestedElderID)
}

func ResolveElderID(ctx context.Context, db *sql.DB, userID, role, requestedElderID string) (string, error) {
	if role == "ELDER" {
		if requestedElderID == "" || requestedElderID == userID {
			return userID, nil
		}
		return "", ErrForbidden
	}
	if role != "CAREGIVER" || requestedElderID == "" {
		return "", ErrForbidden
	}
	var allowed bool
	err := db.QueryRowContext(ctx, `
		SELECT EXISTS(
			SELECT 1 FROM caregiver_elder_links
			WHERE caregiver_id=$1 AND elder_id=$2
		)`, userID, requestedElderID,
	).Scan(&allowed)
	if err != nil || !allowed {
		return "", ErrForbidden
	}
	return requestedElderID, nil
}
