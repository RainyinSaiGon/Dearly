CREATE TABLE IF NOT EXISTS voice_verification_attempts (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    intent      VARCHAR(50) NOT NULL,
    outcome     VARCHAR(32) NOT NULL,
    score       DOUBLE PRECISION,
    audio_hash  CHAR(64) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_voice_verification_attempts_user_intent_created
    ON voice_verification_attempts(user_id, intent, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_voice_verification_attempts_replay
    ON voice_verification_attempts(user_id, intent, audio_hash, created_at DESC);
