-- 007_create_call_logs.sql

CREATE TABLE IF NOT EXISTS call_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    elder_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contact_id      UUID REFERENCES contacts(id) ON DELETE SET NULL,
    direction       VARCHAR(10) NOT NULL CHECK (direction IN ('IN', 'OUT')),
    started_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_seconds INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_call_logs_elder ON call_logs(elder_id);
CREATE INDEX IF NOT EXISTS idx_call_logs_started ON call_logs(started_at);
