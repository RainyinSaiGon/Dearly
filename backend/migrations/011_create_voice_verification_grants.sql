CREATE TABLE IF NOT EXISTS voice_verification_grants (
    token_hash  CHAR(64) PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    intent      VARCHAR(50) NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_voice_grants_user_intent
    ON voice_verification_grants(user_id, intent, expires_at);
