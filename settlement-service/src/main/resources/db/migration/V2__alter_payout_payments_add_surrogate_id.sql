-- Align payout_payments table with PayoutPayment entity (surrogate id + unique pair)

-- 1) Add surrogate id column expected by JPA entity
ALTER TABLE payout_payments
ADD COLUMN IF NOT EXISTS id BIGINT;

-- 2) Create sequence and attach it as default for new rows
CREATE SEQUENCE IF NOT EXISTS payout_payments_id_seq;

ALTER SEQUENCE payout_payments_id_seq
OWNED BY payout_payments.id;

ALTER TABLE payout_payments
ALTER COLUMN id SET DEFAULT nextval('payout_payments_id_seq');

-- 3) Backfill existing rows
UPDATE payout_payments
SET id = nextval('payout_payments_id_seq')
WHERE id IS NULL;

-- 4) Ensure sequence starts after current max(id)
SELECT setval(
  'payout_payments_id_seq',
  COALESCE((SELECT MAX(id) FROM payout_payments), 1),
  true
);

ALTER TABLE payout_payments
ALTER COLUMN id SET NOT NULL;

-- 5) Replace composite primary key with surrogate primary key
ALTER TABLE payout_payments
DROP CONSTRAINT IF EXISTS payout_payments_pkey;

ALTER TABLE payout_payments
ADD CONSTRAINT payout_payments_pkey PRIMARY KEY (id);

-- 6) Preserve uniqueness of logical relation to avoid duplicates
ALTER TABLE payout_payments
ADD CONSTRAINT uq_payout_payments_payout_payment UNIQUE (payout_id, payment_id);

