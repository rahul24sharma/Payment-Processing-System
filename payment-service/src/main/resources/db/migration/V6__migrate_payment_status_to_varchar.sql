-- Align enum storage with JPA EnumType.STRING for cross-database compatibility.
-- Some databases have a partial index predicate referencing enum literals on status.
-- Drop/recreate that index around the type conversion to avoid enum-vs-varchar operator errors.

DROP INDEX IF EXISTS idx_payments_active;

ALTER TABLE payments
    ALTER COLUMN status DROP DEFAULT;

ALTER TABLE payments
    ALTER COLUMN status TYPE VARCHAR(64)
    USING status::text;

ALTER TABLE payments
    ALTER COLUMN status SET DEFAULT 'PENDING';

CREATE INDEX IF NOT EXISTS idx_payments_active
    ON payments(merchant_id, created_at DESC)
    WHERE status IN ('PENDING', 'AUTHORIZED');
