-- 006_create_medication_logs.sql

CREATE TABLE IF NOT EXISTS medication_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medication_id   UUID NOT NULL REFERENCES medications(id) ON DELETE CASCADE,
    scheduled_time  TIMESTAMP WITH TIME ZONE NOT NULL,
    taken_at        TIMESTAMP WITH TIME ZONE,
    snoozed_until   TIMESTAMP WITH TIME ZONE,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('TAKEN', 'PENDING', 'SNOOZED')),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_med_logs_medication ON medication_logs(medication_id);
CREATE INDEX IF NOT EXISTS idx_med_logs_status ON medication_logs(status);
CREATE INDEX IF NOT EXISTS idx_med_logs_scheduled ON medication_logs(scheduled_time);
