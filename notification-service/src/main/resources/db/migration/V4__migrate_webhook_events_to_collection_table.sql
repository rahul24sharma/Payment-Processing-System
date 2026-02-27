-- Migrate webhook endpoint events from postgres array column to collection table.
-- Idempotent so it can run safely across environments.

CREATE TABLE IF NOT EXISTS webhook_endpoint_events (
    endpoint_id UUID NOT NULL REFERENCES webhook_endpoints(id) ON DELETE CASCADE,
    event_type VARCHAR(100) NOT NULL,
    PRIMARY KEY (endpoint_id, event_type)
);

CREATE INDEX IF NOT EXISTS idx_webhook_endpoint_events_event_type
  ON webhook_endpoint_events(event_type);

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'webhook_endpoints'
      AND column_name = 'events'
  ) THEN
    INSERT INTO webhook_endpoint_events (endpoint_id, event_type)
    SELECT w.id, unnest(w.events)
    FROM webhook_endpoints w
    ON CONFLICT DO NOTHING;
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_name = 'webhook_endpoints'
      AND column_name = 'events'
  ) THEN
    ALTER TABLE webhook_endpoints DROP COLUMN events;
  END IF;
END $$;
