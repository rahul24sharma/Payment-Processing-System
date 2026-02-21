package com.payment.service.mapper;

import com.payment.service.dto.request.CreatePaymentRequest;
import com.payment.service.dto.request.PaymentMethodRequest;
import com.payment.service.dto.response.PaymentResponse;
import com.payment.service.entity.Money;
import com.payment.service.entity.Payment;
import com.payment.service.entity.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentMapperTest {
    
    private PaymentMapper mapper;
    
    @BeforeEach
    void setUp() {
        mapper = new PaymentMapper();
    }
    
    @Test
    void shouldMapEntityToResponse() {
        // Given
        Payment payment = Payment.builder()
            .id(UUID.randomUUID())
            .merchantId(UUID.randomUUID())
            .customerId(UUID.randomUUID())
            .amount(Money.of(new BigDecimal("100.00"), "USD"))
            .status(PaymentStatus.AUTHORIZED)
            .fraudScore(new BigDecimal("15.5"))
            .createdAt(Instant.now())
            .authorizedAt(Instant.now())
            .build();
        
        // When
        PaymentResponse response = mapper.toResponse(payment);
        
        // Then
        assertNotNull(response);
        assertEquals(payment.getId().toString(), response.getId());
        assertEquals(10000L, response.getAmount()); // $100.00 = 10000 cents
        assertEquals("usd", response.getCurrency());
        assertEquals("authorized", response.getStatus());
        assertNotNull(response.getFraudDetails());
        assertEquals(new BigDecimal("15.5"), response.getFraudDetails().getScore());
    }
    
    @Test
    void shouldMapRequestToEntity() {
        // Given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("order_id", "ord_123");
        
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .paymentMethod(PaymentMethodRequest.builder()
                .type("card")
                .cardToken("tok_visa_4242")
                .build())
            .metadata(metadata)
            .build();
        
        // When
        Payment payment = mapper.toEntity(request);
        
        // Then
        assertNotNull(payment);
        assertEquals(new BigDecimal("100.00"), payment.getAmount().getAmount());
        assertEquals("USD", payment.getAmount().getCurrency());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(metadata, payment.getMetadata());
    }
    
    @Test
    void shouldHandleNullValuesGracefully() {
        // When
        PaymentResponse response = mapper.toResponse(null);
        
        // Then
        assertNull(response);
    }
    
    @Test
    void shouldMapMinimalResponse() {
        // Given
        Payment payment = Payment.builder()
            .id(UUID.randomUUID())
            .amount(Money.of(new BigDecimal("50.00"), "EUR"))
            .status(PaymentStatus.CAPTURED)
            .createdAt(Instant.now())
            .build();
        
        // When
        PaymentResponse response = mapper.toMinimalResponse(payment);
        
        // Then
        assertNotNull(response);
        assertEquals(5000L, response.getAmount());
        assertEquals("eur", response.getCurrency());
        assertEquals("captured", response.getStatus());
        assertTrue(response.getCaptured());
        // Should not include refunds, fraud details, etc.
        assertNull(response.getRefunds());
    }
}