-- Fix: Change risk_level column in fraud_scores from PostgreSQL native ENUM
-- to VARCHAR so Hibernate 6's @Enumerated(EnumType.STRING) can write to it.
-- PostgreSQL rejects uncast VARCHAR values for native ENUM columns.

ALTER TABLE fraud_scores ALTER COLUMN risk_level TYPE VARCHAR(20);
