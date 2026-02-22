-- This script runs when PostgreSQL container starts

-- Create databases for each service
CREATE DATABASE payment_db;
CREATE DATABASE fraud_db;
CREATE DATABASE ledger_db;
CREATE DATABASE settlement_db;
CREATE DATABASE notification_db;
CREATE DATABASE merchant_db;

-- Grant admin access to all databases
GRANT ALL PRIVILEGES ON DATABASE payment_db TO rahul;
GRANT ALL PRIVILEGES ON DATABASE fraud_db TO rahul;
GRANT ALL PRIVILEGES ON DATABASE ledger_db TO rahul;
GRANT ALL PRIVILEGES ON DATABASE settlement_db TO rahul;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO rahul;
GRANT ALL PRIVILEGES ON DATABASE merchant_db TO rahul;