package com.payment.service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    
    private String eventId;
    private String eventType; // PAYMENT_CREATED, PAYMENT_AUTHORIZED, PAYMENT_CAPTURED, etc.
    private UUID paymentId;
    private UUID merchantId;
    private UUID customerId;
    
    private Long amount;
    private String currency;
    
    private String status;
    private String previousStatus;
    
    private BigDecimal fraudScore;
    
    private Map<String, Object> metadata;
    
    private Instant timestamp;
}