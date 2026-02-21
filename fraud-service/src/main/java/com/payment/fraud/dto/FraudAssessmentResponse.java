package com.payment.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAssessmentResponse {
    
    private BigDecimal score;
    private String riskLevel;
    private String decision; // ALLOW, REVIEW, BLOCK
    private Map<String, Object> factors;
    private String modelVersion;
}