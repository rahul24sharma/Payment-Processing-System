-- Merchant Service Database Schema
-- Version: V1

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_type WHERE typname = 'merchant_status'
  ) THEN
    CREATE TYPE merchant_status AS ENUM (
      'PENDING_REVIEW',
      'ACTIVE',
      'INACTIVE',
      'SUSPENDED'
    );
  END IF;
END $$;
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_type WHERE typname = 'risk_profile'
  ) THEN
    CREATE TYPE risk_profile AS ENUM ('LOW', 'MEDIUM', 'HIGH');
  END IF;
END $$;

-- Merchants
CREATE TABLE IF NOT EXISTS merchants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    -- Business details
    business_name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50),
    
    -- Business address
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(2), -- ISO 3166-1 alpha-2
    
    -- API credentials (hashed!)
    api_key_hash VARCHAR(255) NOT NULL UNIQUE,
    webhook_secret VARCHAR(255),
    
    -- Status
    status merchant_status NOT NULL DEFAULT 'PENDING_REVIEW',
    risk_profile risk_profile NOT NULL DEFAULT 'MEDIUM',
    
    -- Processing fees
    processing_fee_percent DECIMAL(5,4) NOT NULL DEFAULT 0.0290, -- 2.9%
    fixed_fee_cents INTEGER NOT NULL DEFAULT 30, -- $0.30
    
    -- Settlement details
    settlement_account JSONB, -- Bank account details (encrypted)
    settlement_currency VARCHAR(3) DEFAULT 'USD',
    settlement_schedule VARCHAR(50) DEFAULT 'DAILY', -- DAILY, WEEKLY, MONTHLY
    
    -- KYC/Compliance
    kyc_verified BOOLEAN DEFAULT FALSE,
    kyc_verified_at TIMESTAMP,
    tax_id VARCHAR(50),
    
    -- Limits
    daily_volume_limit DECIMAL(19,4),
    monthly_volume_limit DECIMAL(19,4),
    transaction_limit DECIMAL(19,4),
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    activated_at TIMESTAMP,
    suspended_at TIMESTAMP,
    
    created_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_merchants_email ON merchants(email);
CREATE INDEX IF NOT EXISTS idx_merchants_status ON merchants(status);
CREATE INDEX IF NOT EXISTS idx_merchants_api_key ON merchants(api_key_hash);
CREATE INDEX IF NOT EXISTS idx_merchants_created ON merchants(created_at DESC);

-- API keys history (for rotation)
CREATE TABLE IF NOT EXISTS api_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    key_prefix VARCHAR(20) NOT NULL, -- e.g., "sk_live_" or "sk_test_"
    
    name VARCHAR(100), -- User-defined name
    
    is_active BOOLEAN DEFAULT TRUE,
    
    last_used_at TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_api_keys_merchant ON api_keys(merchant_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_hash ON api_keys(key_hash);
CREATE INDEX IF NOT EXISTS idx_api_keys_active ON api_keys(is_active) WHERE is_active = TRUE;

-- Merchant settings
CREATE TABLE IF NOT EXISTS merchant_settings (
    merchant_id UUID PRIMARY KEY REFERENCES merchants(id),
    
    -- Email notifications
    notify_on_payment BOOLEAN DEFAULT TRUE,
    notify_on_refund BOOLEAN DEFAULT TRUE,
    notify_on_chargeback BOOLEAN DEFAULT TRUE,
    
    -- Dashboard preferences
    dashboard_timezone VARCHAR(50) DEFAULT 'UTC',
    dashboard_currency VARCHAR(3) DEFAULT 'USD',
    
    -- Payment page customization
    branding_logo_url VARCHAR(500),
    branding_primary_color VARCHAR(7), -- Hex color
    
    settings_json JSONB,
    
    updated_at TIMESTAMP
);

-- Merchant users (team members)
CREATE TABLE IF NOT EXISTS merchant_users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    
    role VARCHAR(50) NOT NULL, -- OWNER, ADMIN, DEVELOPER, VIEWER
    
    is_active BOOLEAN DEFAULT TRUE,
    
    last_login_at TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    invited_at TIMESTAMP,
    invited_by UUID,
    
    CONSTRAINT unique_merchant_user_email UNIQUE(merchant_id, email)
);

CREATE INDEX IF NOT EXISTS idx_merchant_users_merchant ON merchant_users(merchant_id);
CREATE INDEX IF NOT EXISTS idx_merchant_users_email ON merchant_users(email);

-- Generic updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- Trigger for updated_at
CREATE TRIGGER update_merchants_updated_at BEFORE UPDATE ON merchants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_merchant_settings_updated_at BEFORE UPDATE ON merchant_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE merchants IS 'Merchant accounts and business details';
COMMENT ON COLUMN merchants.api_key_hash IS 'Hashed API key (never store plaintext)';
COMMENT ON COLUMN merchants.settlement_account IS 'Encrypted bank account details for payouts';