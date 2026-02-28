ALTER TABLE merchants
    ADD COLUMN IF NOT EXISTS password_reset_token_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS password_reset_token_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS password_reset_requested_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_merchants_password_reset_token_hash
    ON merchants (password_reset_token_hash);
