-- 002_create_elder_profiles.sql

CREATE TABLE IF NOT EXISTS elder_profiles (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    health_status_override  VARCHAR(20) CHECK (health_status_override IN ('NORMAL', 'WARNING', 'CRITICAL')),
    created_at              TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- health_status_auto is computed at query time from medication_logs, not stored.
-- health_status_effective = COALESCE(health_status_override, computed_auto)
