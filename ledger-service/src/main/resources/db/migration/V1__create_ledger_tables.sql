-- Ledger Service Database Schema (Double-Entry Bookkeeping)
-- Version: V1

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Account types
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'account_type') THEN
    CREATE TYPE account_type AS ENUM (
      'CUSTOMER','MERCHANT','PLATFORM_FEE','PROCESSOR_FEE','RESERVE','BANK_SETTLEMENT'
    );
  END IF;
END $$;


-- Ledger entries (double-entry bookkeeping)
CREATE TABLE ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    
    entry_group_id UUID NOT NULL, -- Groups related debit/credit entries
    
    account_id UUID NOT NULL,
    account_type account_type NOT NULL,
    
    -- Either debit OR credit, never both
    debit_amount DECIMAL(19,4) NOT NULL DEFAULT 0 CHECK (debit_amount >= 0),
    credit_amount DECIMAL(19,4) NOT NULL DEFAULT 0 CHECK (credit_amount >= 0),
    
    currency VARCHAR(3) NOT NULL,
    
    -- Reference to source transaction
    payment_id UUID,
    refund_id UUID,
    settlement_id UUID,
    
    description TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Ensure only debit OR credit, not both
    CONSTRAINT chk_debit_or_credit CHECK (
        (debit_amount > 0 AND credit_amount = 0) OR
        (credit_amount > 0 AND debit_amount = 0)
    )
);

-- Partition by month for better performance
CREATE INDEX idx_ledger_entry_group ON ledger_entries(entry_group_id);
CREATE INDEX idx_ledger_account ON ledger_entries(account_id);
CREATE INDEX idx_ledger_account_date ON ledger_entries(account_id, created_at DESC);
CREATE INDEX idx_ledger_payment ON ledger_entries(payment_id);
CREATE INDEX idx_ledger_created ON ledger_entries(created_at DESC);
CREATE INDEX idx_ledger_account_type ON ledger_entries(account_type);

-- Account balances (materialized view for performance)
CREATE TABLE account_balances (
    account_id UUID PRIMARY KEY,
    account_type account_type NOT NULL,
    
    balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_account UNIQUE(account_id, currency)
);

CREATE INDEX idx_account_balances_type ON account_balances(account_type);
CREATE INDEX idx_account_balances_updated ON account_balances(last_updated DESC);

-- Reconciliation reports
CREATE TABLE reconciliation_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    
    report_date DATE NOT NULL,
    
    total_entry_groups INTEGER,
    imbalanced_groups INTEGER,
    balance_mismatches INTEGER,
    
    is_balanced BOOLEAN NOT NULL,
    
    details JSONB,
    
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generated_by VARCHAR(255)
);

CREATE INDEX idx_reconciliation_date ON reconciliation_reports(report_date DESC);
CREATE INDEX idx_reconciliation_balanced ON reconciliation_reports(is_balanced);

-- Function to validate ledger balance for an entry group
CREATE OR REPLACE FUNCTION validate_ledger_balance()
RETURNS TRIGGER AS $$
DECLARE
    total_debits DECIMAL(19,4);
    total_credits DECIMAL(19,4);
BEGIN
    SELECT 
        COALESCE(SUM(debit_amount), 0),
        COALESCE(SUM(credit_amount), 0)
    INTO total_debits, total_credits
    FROM ledger_entries
    WHERE entry_group_id = NEW.entry_group_id;
    
    IF total_debits != total_credits THEN
        RAISE EXCEPTION 'Ledger imbalance detected: debits=%, credits=%', 
            total_debits, total_credits;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Note: We'll validate balance after all entries are inserted, not on each insert
-- CREATE TRIGGER validate_ledger_balance_trigger
--     AFTER INSERT ON ledger_entries
--     FOR EACH ROW
--     EXECUTE FUNCTION validate_ledger_balance();

COMMENT ON TABLE ledger_entries IS 'Double-entry accounting ledger';
COMMENT ON COLUMN ledger_entries.entry_group_id IS 'Groups related debit/credit entries for a single transaction';
COMMENT ON COLUMN ledger_entries.debit_amount IS 'Debit amount (money out of account)';
COMMENT ON COLUMN ledger_entries.credit_amount IS 'Credit amount (money into account)';
COMMENT ON TABLE account_balances IS 'Materialized view of current account balances';