package com.payment.service.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {
    
    @Test
    void shouldCreatePaymentWithMoney() {
        // Given
        Money amount = Money.of(new BigDecimal("100.00"), "USD");
        
        // When
        Payment payment = Payment.builder()
            .merchantId(UUID.randomUUID())
            .customerId(UUID.randomUUID())
            .amount(amount)
            .status(PaymentStatus.PENDING)
            .build();
        
        // Then
        assertNotNull(payment);
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(new BigDecimal("100.00"), payment.getAmount().getAmount());
        assertEquals("USD", payment.getAmount().getCurrency());
    }
    
    @Test
    void shouldAllowTransitionFromPendingToAuthorized() {
        // Given
        Payment payment = Payment.builder()
            .status(PaymentStatus.PENDING)
            .build();
        
        // When & Then
        assertTrue(payment.canTransitionTo(PaymentStatus.AUTHORIZED));
        assertFalse(payment.canTransitionTo(PaymentStatus.CAPTURED));
    }
    
    @Test
    void shouldCalculateRemainingRefundableAmount() {
        // Given
        Money amount = Money.of(new BigDecimal("100.00"), "USD");
        Payment payment = Payment.builder()
            .amount(amount)
            .status(PaymentStatus.CAPTURED)
            .build();
        
        Refund refund = Refund.builder()
            .amount(Money.of(new BigDecimal("30.00"), "USD"))
            .status(RefundStatus.SUCCEEDED)
            .build();
        
        payment.addRefund(refund);
        
        // When
        Money remaining = payment.getRemainingRefundableAmount();
        
        // Then
        assertEquals(new BigDecimal("70.00"), remaining.getAmount());
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, payment.getStatus());
    }
    
    @Test
    void shouldMarkPaymentAsFullyRefunded() {
        // Given
        Money amount = Money.of(new BigDecimal("100.00"), "USD");
        Payment payment = Payment.builder()
            .amount(amount)
            .status(PaymentStatus.CAPTURED)
            .build();
        
        Refund refund = Refund.builder()
            .amount(Money.of(new BigDecimal("100.00"), "USD"))
            .status(RefundStatus.SUCCEEDED)
            .build();
        
        // When
        payment.addRefund(refund);
        
        // Then
        assertTrue(payment.getRemainingRefundableAmount().isZero());
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }
}