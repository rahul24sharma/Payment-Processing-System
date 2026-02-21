-- Seed initial fraud rules

INSERT INTO fraud_rules (id, rule_name, rule_type, conditions, action, score_impact, priority, is_active, created_by) VALUES
(uuid_generate_v4(), 'High Amount Transaction', 'AMOUNT', 
 '{"threshold": 100000, "currency": "USD"}'::jsonb, 
 'SCORE', 30, 100, true, 'SYSTEM'),

(uuid_generate_v4(), 'Very High Amount Transaction', 'AMOUNT', 
 '{"threshold": 500000, "currency": "USD"}'::jsonb, 
 'REVIEW', 50, 150, true, 'SYSTEM'),

(uuid_generate_v4(), 'Extreme Amount - Auto Block', 'AMOUNT', 
 '{"threshold": 1000000, "currency": "USD"}'::jsonb, 
 'BLOCK', 100, 200, true, 'SYSTEM');

-- Add sample blocklist entries (for testing)
INSERT INTO blocklists (id, type, value, reason, is_active, created_by) VALUES
(uuid_generate_v4(), 'EMAIL', 'fraud@example.com', 'Known fraudster', true, 'SYSTEM'),
(uuid_generate_v4(), 'IP', '192.168.999.999', 'Suspicious IP', true, 'SYSTEM');

COMMENT ON TABLE fraud_rules IS 'Configurable fraud detection rules';
COMMENT ON TABLE blocklists IS 'Blocked entities (emails, IPs, etc.)';