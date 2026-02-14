-- Payments Service Database Schema
-- Version: V1 (idempotent / rerunnable)

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =========================
-- ENUMS (idempotent)
-- =========================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_status') THEN
    CREATE TYPE payment_status AS ENUM (
      'PENDING',
      'AUTHORIZED',
      'CAPTURED',
      'VOID',
      'REFUNDED',
      'PARTIALLY_REFUNDED',
      'FAILED',
      'DECLINED',
      'EXPIRED'
    );
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_method_type') THEN
    CREATE TYPE payment_method_type AS ENUM (
      'CARD',
      'BANK_ACCOUNT',
      'WALLET'
    );
  END IF;
END $$;

-- =========================
-- TABLE: payments
-- =========================
CREATE TABLE IF NOT EXISTS payments (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  merchant_id UUID NOT NULL,
  customer_id UUID,

  -- Idempotency
  idempotency_key VARCHAR(225) UNIQUE,

  -- Amount details
  amount DECIMAL(19,4) NOT NULL CHECK (amount >= 0),
  currency VARCHAR(3) NOT NULL,

  -- Status
  status payment_status NOT NULL DEFAULT 'PENDING',

  -- Payment method
  payment_method_id UUID,

  -- Processor details
  processor VARCHAR(50),
  processor_payment_id VARCHAR(255),

  -- Fraud
  fraud_score DECIMAL(5,2),

  -- Metadata
  metadata JSONB,

  -- Failure details
  failure_reason TEXT,
  failure_code VARCHAR(50),

  -- Optimistic locking
  version INTEGER NOT NULL DEFAULT 1,

  -- Timestamps
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  authorized_at TIMESTAMP,
  captured_at TIMESTAMP,

  -- Additional validation
  CONSTRAINT chk_currency_format CHECK (currency ~ '^[A-Z]{3}$')
);

-- Indexes for payments
CREATE INDEX IF NOT EXISTS idx_payments_merchant_id ON payments(merchant_id);
CREATE INDEX IF NOT EXISTS idx_payments_customer_id ON payments(customer_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payments_merchant_created ON payments(merchant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payments_merchant_status ON payments(merchant_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payments_processor_id ON payments(processor_payment_id);
CREATE INDEX IF NOT EXISTS idx_payments_idempotency ON payments(idempotency_key);

-- Partial index for "active" payments
CREATE INDEX IF NOT EXISTS idx_payments_active
  ON payments(merchant_id, created_at DESC)
  WHERE status IN ('PENDING', 'AUTHORIZED');

-- GIN index for JSONB metadata
CREATE INDEX IF NOT EXISTS idx_payments_metadata ON payments USING GIN(metadata);

-- =========================
-- TABLE: payment_methods
-- =========================
CREATE TABLE IF NOT EXISTS payment_methods (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  customer_id UUID NOT NULL,

  type payment_method_type NOT NULL,

  -- tokenized data (never store raw card data)
  token VARCHAR(225) NOT NULL,

  -- Card details (display only)
  card_brand VARCHAR(50),
  card_last4 VARCHAR(4),
  card_exp_month INTEGER CHECK (card_exp_month BETWEEN 1 AND 12),
  card_exp_year INTEGER CHECK (card_exp_year >= EXTRACT(YEAR FROM CURRENT_DATE)),

  -- Bank account details (tokenized)
  bank_name VARCHAR(225),
  bank_account_last4 VARCHAR(4),

  -- Status
  is_default BOOLEAN DEFAULT FALSE,
  is_active BOOLEAN DEFAULT TRUE,

  -- Timestamps
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,

  CONSTRAINT chk_card_last4 CHECK (card_last4 IS NULL OR card_last4 ~ '^[0-9]{4}$'),
  CONSTRAINT chk_bank_last4 CHECK (bank_account_last4 IS NULL OR bank_account_last4 ~ '^[0-9]{4}$')
);

CREATE INDEX IF NOT EXISTS idx_payment_methods_customer ON payment_methods(customer_id);
CREATE INDEX IF NOT EXISTS idx_payment_methods_token ON payment_methods(token);

-- =========================
-- TABLE: refunds
-- =========================
CREATE TABLE IF NOT EXISTS refunds (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  payment_id UUID NOT NULL REFERENCES payments(id),

  amount DECIMAL(19,4) NOT NULL CHECK (amount > 0),
  currency VARCHAR(3) NOT NULL,

  reason VARCHAR(225),

  status VARCHAR(50) NOT NULL DEFAULT 'PENDING',

  -- Processor details
  processor_refund_id VARCHAR(225),

  -- Failure details
  failure_reason TEXT,

  -- Timestamps
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  completed_at TIMESTAMP,
  failed_at TIMESTAMP,

  CONSTRAINT chk_refund_currency_format CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX IF NOT EXISTS idx_refunds_payment ON refunds(payment_id);
CREATE INDEX IF NOT EXISTS idx_refunds_created ON refunds(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_refunds_status ON refunds(status);

-- =========================
-- TABLE: payment_events (audit trail)
-- =========================
CREATE TABLE IF NOT EXISTS payment_events (
  id BIGSERIAL PRIMARY KEY,
  payment_id UUID NOT NULL,

  event_type VARCHAR(50) NOT NULL,
  previous_state VARCHAR(50),
  new_state VARCHAR(50),

  metadata JSONB,

  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_payment_events_payment ON payment_events(payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_events_type ON payment_events(event_type);
CREATE INDEX IF NOT EXISTS idx_payment_events_created ON payment_events(created_at DESC);

-- =========================
-- updated_at trigger function + triggers (idempotent)
-- =========================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_payments_updated_at ON payments;
CREATE TRIGGER update_payments_updated_at
BEFORE UPDATE ON payments
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_payment_methods_updated_at ON payment_methods;
CREATE TRIGGER update_payment_methods_updated_at
BEFORE UPDATE ON payment_methods
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_refunds_updated_at ON refunds;
CREATE TRIGGER update_refunds_updated_at
BEFORE UPDATE ON refunds
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =========================
-- Comments
-- =========================
COMMENT ON TABLE payments IS 'Core payments table storing all payment transactions';
COMMENT ON COLUMN payments.idempotency_key IS 'Unique key to prevent duplicate payments';
COMMENT ON COLUMN payments.amount IS 'Payment amount (decimal).';
COMMENT ON COLUMN payments.version IS 'Version for optimistic locking';
COMMENT ON TABLE payment_methods IS 'Tokenized payment methods (never stores raw card data)';
COMMENT ON COLUMN payment_methods.token IS 'Tokenized payment method from processor';
