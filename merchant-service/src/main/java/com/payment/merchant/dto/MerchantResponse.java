package com.payment.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantResponse {
    private String id;
    private String businessName;
    private String email;
    private String phone;
    private String website;
    private Map<String, Object> bankAccount;
    private Map<String, Object> notifications;
    private String status;
    private String riskProfile;
    private Map<String, Object> settings;
    private Instant createdAt;
}
