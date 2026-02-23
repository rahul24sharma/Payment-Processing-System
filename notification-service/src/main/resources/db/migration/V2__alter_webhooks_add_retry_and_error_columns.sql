-- Align webhooks table with Webhook entity fields introduced after V1

ALTER TABLE webhooks
ADD COLUMN IF NOT EXISTS last_error TEXT;

ALTER TABLE webhooks
ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP;

