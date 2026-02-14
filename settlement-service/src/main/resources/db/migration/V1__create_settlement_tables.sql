-- Settlement Service Database Schema
-- Version: V1 (idempotent / rerunnable)

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =========================
-- ENUMS (idempotent)
-- =========================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'settlement_status') THEN
    CREATE TYPE settlement_status AS ENUM ('PROCESSING', 'COMPLETED', 'PARTIALLY_COMPLETED', 'FAILED');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payout_status') THEN
    CREATE TYPE payout_status AS ENUM ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED');
  END IF;
END $$;

-- =========================
-- updated_at trigger function (per DB)
-- =========================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =========================
-- settlement_batches
-- =========================
CREATE TABLE IF NOT EXISTS settlement_batches (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

  settlement_date DATE NOT NULL,
  capture_date DATE NOT NULL, -- Payments captured on this date (T-2)

  total_payments INTEGER NOT NULL DEFAULT 0,
  total_payouts INTEGER NOT NULL DEFAULT 0,

  status settlement_status NOT NULL DEFAULT 'PROCESSING',

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP,

  CONSTRAINT unique_settlement_date UNIQUE (settlement_date)
);

CREATE INDEX IF NOT EXISTS idx_settlement_batches_date
  ON settlement_batches(settlement_date DESC);

CREATE INDEX IF NOT EXISTS idx_settlement_batches_status
  ON settlement_batches(status);

-- =========================
-- payouts
-- =========================
CREATE TABLE IF NOT EXISTS payouts (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  batch_id UUID REFERENCES settlement_batches(id),

  merchant_id UUID NOT NULL,
  settlement_date DATE NOT NULL,

  -- Amounts
  total_amount DECIMAL(19,4) NOT NULL,         -- Total captured
  fee_amount DECIMAL(19,4) NOT NULL,           -- Platform fees
  net_amount DECIMAL(19,4) NOT NULL,           -- After fees
  reserve_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
  payout_amount DECIMAL(19,4) NOT NULL,        -- Actually paid out

  currency VARCHAR(3) NOT NULL,

  payment_count INTEGER NOT NULL,

  -- Bank transfer details
  bank_transfer_id VARCHAR(255),
  bank_account_id UUID,

  status payout_status NOT NULL DEFAULT 'PENDING',

  failure_reason TEXT,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  completed_at TIMESTAMP,
  failed_at TIMESTAMP,

  CONSTRAINT chk_currency_format CHECK (currency ~ '^[A-Z]{3}$'),
  CONSTRAINT chk_amounts CHECK (
    total_amount >= 0 AND
    fee_amount >= 0 AND
    net_amount >= 0 AND
    reserve_amount >= 0 AND
    payout_amount >= 0 AND
    payout_amount = net_amount - reserve_amount
  )
);

CREATE INDEX IF NOT EXISTS idx_payouts_batch ON payouts(batch_id);
CREATE INDEX IF NOT EXISTS idx_payouts_merchant ON payouts(merchant_id);
CREATE INDEX IF NOT EXISTS idx_payouts_date ON payouts(settlement_date DESC);
CREATE INDEX IF NOT EXISTS idx_payouts_status ON payouts(status);
CREATE INDEX IF NOT EXISTS idx_payouts_created ON payouts(created_at DESC);

-- =========================
-- payout_payments (join table)
-- =========================
CREATE TABLE IF NOT EXISTS payout_payments (
  payout_id UUID NOT NULL REFERENCES payouts(id),
  payment_id UUID NOT NULL,
  PRIMARY KEY (payout_id, payment_id)
);

CREATE INDEX IF NOT EXISTS idx_payout_payments_payout ON payout_payments(payout_id);
CREATE INDEX IF NOT EXISTS idx_payout_payments_payment ON payout_payments(payment_id);

-- =========================
-- Trigger for payouts.updated_at (rerunnable)
-- =========================
DROP TRIGGER IF EXISTS update_payouts_updated_at ON payouts;
CREATE TRIGGER update_payouts_updated_at
BEFORE UPDATE ON payouts
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =========================
-- Comments
-- =========================
COMMENT ON TABLE settlement_batches IS 'Daily settlement batch runs (T+2 settlement)';
COMMENT ON TABLE payouts IS 'Individual merchant payouts';
COMMENT ON COLUMN payouts.reserve_amount IS 'Amount held as reserve for potential chargebacks (typically 5%)';
