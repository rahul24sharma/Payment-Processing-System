package com.payment.fraud.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAssessmentRequest {
    
    @NotNull
    private UUID paymentId;
    
    @NotNull
    private UUID merchantId;
    
    private UUID customerId;
    
    @NotNull
    private Long amount;
    
    @NotNull
    private String currency;
    
    private UUID paymentMethodId;
    
    private String ipAddress;
    
    private String deviceId;
    
    private String userAgent;
    
    private Map<String, Object> metadata;
}