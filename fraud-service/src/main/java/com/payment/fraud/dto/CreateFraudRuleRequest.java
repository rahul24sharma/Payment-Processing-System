package com.payment.fraud.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

@Data
public class CreateFraudRuleRequest {

    @NotBlank
    private String ruleName;

    @NotBlank
    private String ruleType;

    @NotEmpty
    private Map<String, Object> conditions;

    @NotBlank
    private String action;

    @Min(0)
    @Max(100)
    private Integer scoreImpact;

    @Min(0)
    @Max(1000)
    private Integer priority;

    private Boolean isActive;

    private String createdBy;
}
