package com.payment.fraud.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Value
@Builder
public class FraudRuleResponse {
    UUID id;
    String ruleName;
    String ruleType;
    Map<String, Object> conditions;
    String action;
    Integer scoreImpact;
    Integer priority;
    Boolean isActive;
    String createdBy;
    Instant createdAt;
    Instant updatedAt;
}
