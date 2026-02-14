-- Notification Service Database Schema
-- Version: V1 (executable & rerunnable)

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ENUM (idempotent)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_type WHERE typname = 'webhook_status'
  ) THEN
    CREATE TYPE webhook_status AS ENUM ('PENDING', 'DELIVERED', 'FAILED');
  END IF;
END $$;

-- updated_at trigger function (per DB)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Webhook endpoints
CREATE TABLE IF NOT EXISTS webhook_endpoints (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID NOT NULL,

    url VARCHAR(500) NOT NULL,
    secret VARCHAR(255) NOT NULL,

    events TEXT[] NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT unique_merchant_url UNIQUE (merchant_id, url)
);

CREATE INDEX IF NOT EXISTS idx_webhook_endpoints_merchant
  ON webhook_endpoints(merchant_id);

CREATE INDEX IF NOT EXISTS idx_webhook_endpoints_active
  ON webhook_endpoints(is_active)
  WHERE is_active = TRUE;

-- Webhooks
CREATE TABLE IF NOT EXISTS webhooks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID NOT NULL,
    endpoint_id UUID REFERENCES webhook_endpoints(id),

    url VARCHAR(500) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,

    status webhook_status NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,

    last_response_code INTEGER,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP,
    failed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_webhooks_merchant ON webhooks(merchant_id);
CREATE INDEX IF NOT EXISTS idx_webhooks_endpoint ON webhooks(endpoint_id);
CREATE INDEX IF NOT EXISTS idx_webhooks_status ON webhooks(status);
CREATE INDEX IF NOT EXISTS idx_webhooks_created ON webhooks(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_webhooks_pending
  ON webhooks(status) WHERE status = 'PENDING';

-- Dead letter queue
CREATE TABLE IF NOT EXISTS webhook_dead_letter_queue (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    webhook_id UUID NOT NULL,
    merchant_id UUID NOT NULL,

    url VARCHAR(500) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,

    total_attempts INTEGER NOT NULL,
    last_error TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retried_at TIMESTAMP,
    retry_successful BOOLEAN
);

CREATE INDEX IF NOT EXISTS idx_webhook_dlq_merchant
  ON webhook_dead_letter_queue(merchant_id);

CREATE INDEX IF NOT EXISTS idx_webhook_dlq_created
  ON webhook_dead_letter_queue(created_at DESC);

-- Trigger (drop + create)
DROP TRIGGER IF EXISTS update_webhook_endpoints_updated_at ON webhook_endpoints;
CREATE TRIGGER update_webhook_endpoints_updated_at
BEFORE UPDATE ON webhook_endpoints
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comments
COMMENT ON TABLE webhook_endpoints IS 'Merchant webhook endpoint configurations';
COMMENT ON TABLE webhooks IS 'Webhook delivery records with retry attempts';
COMMENT ON TABLE webhook_dead_letter_queue IS 'Failed webhooks after all retry attempts exhausted';
