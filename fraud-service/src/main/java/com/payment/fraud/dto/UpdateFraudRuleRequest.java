package com.payment.fraud.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.Map;

@Data
public class UpdateFraudRuleRequest {
    private String ruleName;
    private String ruleType;
    private Map<String, Object> conditions;
    private String action;

    @Min(0)
    @Max(100)
    private Integer scoreImpact;

    @Min(0)
    @Max(1000)
    private Integer priority;

    private Boolean isActive;
}
