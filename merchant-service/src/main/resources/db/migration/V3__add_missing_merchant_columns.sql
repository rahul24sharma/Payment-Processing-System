-- Add password_hash and settings columns missing from initial schema
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS settings JSONB;
