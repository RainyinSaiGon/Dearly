-- 003_create_caregiver_elder_links.sql

CREATE TABLE IF NOT EXISTS caregiver_elder_links (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    caregiver_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    elder_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(caregiver_id, elder_id)
);

CREATE INDEX idx_links_caregiver ON caregiver_elder_links(caregiver_id);
CREATE INDEX idx_links_elder ON caregiver_elder_links(elder_id);
