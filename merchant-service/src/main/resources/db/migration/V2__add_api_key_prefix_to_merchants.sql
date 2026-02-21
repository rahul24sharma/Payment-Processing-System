-- Add api_key_prefix column to merchants table
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS api_key_prefix VARCHAR(50);
