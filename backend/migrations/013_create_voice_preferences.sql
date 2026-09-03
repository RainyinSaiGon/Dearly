CREATE TABLE IF NOT EXISTS voice_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    reminder_style VARCHAR(20) NOT NULL DEFAULT 'GENTLE'
        CHECK (reminder_style IN ('GENTLE', 'DIRECT')),
    speech_rate DOUBLE PRECISION NOT NULL DEFAULT 0.85
        CHECK (speech_rate >= 0.5 AND speech_rate <= 1.5),
    preferred_contact_id UUID REFERENCES contacts(id) ON DELETE SET NULL,
    include_daily_schedule BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_voice_preferences_preferred_contact
    ON voice_preferences(preferred_contact_id);
