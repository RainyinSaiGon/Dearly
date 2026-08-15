-- 001_create_users.sql
-- Runs on first docker-compose up via /docker-entrypoint-initdb.d

CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number    VARCHAR(20) UNIQUE,
    email           VARCHAR(255) UNIQUE,
    name            VARCHAR(100) NOT NULL,
    age             INT,
    city            VARCHAR(100),
    role            VARCHAR(20) NOT NULL CHECK (role IN ('ELDER', 'CAREGIVER')),
    avatar_url      TEXT,
    fcm_token       TEXT,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone_number);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
