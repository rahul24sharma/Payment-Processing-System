ALTER TABLE merchants
    ADD COLUMN IF NOT EXISTS role VARCHAR(32) NOT NULL DEFAULT 'ADMIN';

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    merchant_id UUID,
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(255),
    actor_role VARCHAR(32),
    action VARCHAR(128) NOT NULL,
    target_type VARCHAR(64),
    target_id VARCHAR(255),
    outcome VARCHAR(32) NOT NULL,
    details_json TEXT,
    ip_address VARCHAR(64),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_merchant_created_at
    ON audit_logs (merchant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_action_created_at
    ON audit_logs (action, created_at DESC);
