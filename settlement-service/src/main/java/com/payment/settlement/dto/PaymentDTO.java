package com.payment.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    private UUID id;
    private UUID merchantId;
    private UUID customerId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private Instant capturedAt;
}