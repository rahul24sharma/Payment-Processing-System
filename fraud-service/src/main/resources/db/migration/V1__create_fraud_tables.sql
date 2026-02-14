-- Fraud Service Database Schema
-- Version: V1

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Fraud risk levels
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'risk_level') THEN
        CREATE TYPE risk_level AS ENUM ('VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL');
    END IF;
END $$;

-- Fraud scores table
CREATE TABLE IF NOT EXISTS fraud_scores (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_id UUID NOT NULL,
    
    -- Score (0-100)
    score DECIMAL(5,2) NOT NULL CHECK (score >= 0 AND score <= 100),
    risk_level risk_level NOT NULL,
    
    -- Individual component scores
    velocity_score DECIMAL(5,2),
    rule_score DECIMAL(5,2),
    ml_score DECIMAL(5,2),
    
    -- Factors contributing to score
    factors JSONB,
    
    -- Model version
    model_version VARCHAR(50),
    
    -- Decision
    decision VARCHAR(50) NOT NULL, -- ALLOW, REVIEW, BLOCK
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_fraud_scores_payment ON fraud_scores(payment_id);
CREATE INDEX IF NOT EXISTS idx_fraud_scores_risk ON fraud_scores(risk_level);
CREATE INDEX IF NOT EXISTS idx_fraud_scores_score ON fraud_scores(score DESC);
CREATE INDEX IF NOT EXISTS idx_fraud_scores_created ON fraud_scores(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_fraud_scores_factors ON fraud_scores USING GIN(factors);

-- Fraud rules table
CREATE TABLE IF NOT EXISTS fraud_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    rule_name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(50) NOT NULL, -- VELOCITY, AMOUNT, GEOLOCATION, PATTERN
    
    -- Rule conditions (stored as JSON)
    conditions JSONB NOT NULL,
    
    -- Action to take
    action VARCHAR(50) NOT NULL, -- BLOCK, REVIEW, ALLOW, SCORE
    score_impact INTEGER, -- Points to add to fraud score
    
    -- Priority (higher = evaluated first)
    priority INTEGER DEFAULT 100,
    
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_fraud_rules_type ON fraud_rules(rule_type);
CREATE INDEX IF NOT EXISTS idx_fraud_rules_active ON fraud_rules(is_active) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_fraud_rules_priority ON fraud_rules(priority DESC);


-- Velocity counters (for rate limiting checks)
CREATE TABLE IF NOT EXISTS velocity_counters (
    id VARCHAR(255) PRIMARY KEY, -- e.g., "card:4242:1h", "ip:192.168.1.1:24h"
    
    counter INTEGER NOT NULL DEFAULT 0,
    
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);


CREATE INDEX IF NOT EXISTS idx_velocity_window ON velocity_counters(window_end);
CREATE INDEX IF NOT EXISTS idx_velocity_created ON velocity_counters(created_at);


-- Cleanup old velocity counters (run daily)
-- DELETE FROM velocity_counters WHERE window_end < NOW() - INTERVAL '7 days';

-- Blocklists
CREATE TABLE IF NOT EXISTS blocklists (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    type VARCHAR(50) NOT NULL, -- EMAIL, CARD_BIN, IP, DEVICE_ID
    value VARCHAR(255) NOT NULL,
    
    reason TEXT,
    
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    created_by VARCHAR(255),
    
    CONSTRAINT unique_blocklist_entry UNIQUE(type, value)
);

CREATE INDEX IF NOT EXISTS idx_blocklists_type_value ON blocklists(type, value);
CREATE INDEX IF NOT EXISTS idx_blocklists_active ON blocklists(is_active) WHERE is_active = TRUE;


-- Allowlists (trusted entities)
CREATE TABLE IF NOT EXISTS allowlists (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    type VARCHAR(50) NOT NULL, -- EMAIL, CARD_BIN, IP, MERCHANT_ID
    value VARCHAR(255) NOT NULL,
    
    reason TEXT,
    
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    
    CONSTRAINT unique_allowlist_entry UNIQUE(type, value)
);

CREATE INDEX IF NOT EXISTS idx_allowlists_type_value ON allowlists(type, value);
CREATE INDEX IF NOT EXISTS idx_allowlists_active ON allowlists(is_active) WHERE is_active = TRUE;

-- Generic updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for updated_at
CREATE TRIGGER update_fraud_rules_updated_at BEFORE UPDATE ON fraud_rules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_velocity_counters_updated_at BEFORE UPDATE ON velocity_counters
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Sample fraud rules
INSERT INTO fraud_rules (rule_name, rule_type, conditions, action, score_impact, priority) VALUES
('High Amount Transaction', 'AMOUNT', '{"threshold": 100000, "currency": "USD"}', 'REVIEW', 30, 100),
('Velocity - Card Attempts', 'VELOCITY', '{"window": "1h", "threshold": 5, "key": "card"}', 'BLOCK', 50, 200),
('Velocity - IP Attempts', 'VELOCITY', '{"window": "24h", "threshold": 20, "key": "ip"}', 'REVIEW', 40, 150);

COMMENT ON TABLE fraud_scores IS 'Fraud assessment results for each payment';
COMMENT ON TABLE fraud_rules IS 'Configurable fraud detection rules';
COMMENT ON TABLE velocity_counters IS 'Rate limiting counters for velocity checks';