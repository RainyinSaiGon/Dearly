CREATE TABLE IF NOT EXISTS caregiver_link_codes (
    elder_id       UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    code_hash      CHAR(64) NOT NULL UNIQUE,
    expires_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_caregiver_link_codes_expires
    ON caregiver_link_codes(expires_at);
