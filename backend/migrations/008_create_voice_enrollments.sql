-- 008_create_voice_enrollments.sql

CREATE TABLE IF NOT EXISTS voice_enrollments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    embedding_vector    BYTEA NOT NULL,      -- serialized float32 array (192-dim ECAPA-TDNN)
    phrase_index        INT NOT NULL,         -- 0..4 for the 5 enrollment phrases
    audio_url           TEXT,                 -- GCS/local path (cleared after embedding computed)
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_voice_enrollment_user ON voice_enrollments(user_id);
CREATE UNIQUE INDEX idx_voice_enrollment_user_phrase ON voice_enrollments(user_id, phrase_index);
