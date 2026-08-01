-- 005_create_medications.sql

CREATE TABLE IF NOT EXISTS medications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    elder_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name                VARCHAR(200) NOT NULL,
    frequency_per_day   INT NOT NULL DEFAULT 1,
    time_slots          JSONB NOT NULL DEFAULT '[]',   -- e.g. ["07:00","13:00","19:00"]
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_medications_elder ON medications(elder_id);
