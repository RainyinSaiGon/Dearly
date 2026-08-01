package database

import (
	"database/sql"
	"fmt"
	"os"

	// PostgreSQL driver — blank import registers it with database/sql
	_ "github.com/lib/pq"
)

// Connect opens a PostgreSQL connection using DATABASE_URL from the environment.
//
// TODO(W2):
//  1. Call this from main.go during startup
//  2. Pass the *sql.DB into every service that needs DB access
//  3. Add connection pool tuning:
//       db.SetMaxOpenConns(25)
//       db.SetMaxIdleConns(5)
//       db.SetConnMaxLifetime(5 * time.Minute)
func Connect() (*sql.DB, error) {
	dsn := os.Getenv("DATABASE_URL")
	if dsn == "" {
		return nil, fmt.Errorf("DATABASE_URL environment variable is not set")
	}

	db, err := sql.Open("postgres", dsn)
	if err != nil {
		return nil, fmt.Errorf("failed to open database connection: %w", err)
	}

	// TODO(W2): Add pool settings here (see above)

	if err := db.Ping(); err != nil {
		return nil, fmt.Errorf("database ping failed: %w", err)
	}

	return db, nil
}
