-- 004_create_contacts.sql

CREATE TABLE IF NOT EXISTS contacts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    elder_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    nickname        VARCHAR(100),
    full_name       VARCHAR(200) NOT NULL,
    phone_number    VARCHAR(20) NOT NULL,
    relationship    VARCHAR(50),
    call_method     VARCHAR(20) NOT NULL DEFAULT 'PHONE' CHECK (call_method IN ('PHONE', 'ZALO_VIDEO')),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_contacts_elder ON contacts(elder_id);
