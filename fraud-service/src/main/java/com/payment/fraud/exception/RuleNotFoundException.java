package com.payment.fraud.exception;

import java.util.UUID;

public class RuleNotFoundException extends FraudException {
    
    public RuleNotFoundException(UUID ruleId) {
        super(
            String.format("Fraud rule not found: %s", ruleId),
            "rule_not_found"
        );
        addDetail("rule_id", ruleId.toString());
    }
}