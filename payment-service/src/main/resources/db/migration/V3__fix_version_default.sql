-- Hibernate 6 initializes @Version fields to 0, not 1.
-- Align the DB default so new rows are consistent with Hibernate's expectations.
ALTER TABLE payments ALTER COLUMN version SET DEFAULT 0;
