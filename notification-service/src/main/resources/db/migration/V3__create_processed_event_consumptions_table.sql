CREATE TABLE IF NOT EXISTS processed_event_consumptions (
    event_key VARCHAR(255) PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    event_id VARCHAR(255),
    partition_id INTEGER,
    offset_value BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_processed_event_consumptions_topic_created_at
    ON processed_event_consumptions (topic, created_at);
