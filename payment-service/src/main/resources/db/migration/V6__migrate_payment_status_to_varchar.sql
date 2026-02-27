-- Align enum storage with JPA EnumType.STRING for cross-database compatibility.
ALTER TABLE payments
    ALTER COLUMN status TYPE VARCHAR(64)
    USING status::text;
