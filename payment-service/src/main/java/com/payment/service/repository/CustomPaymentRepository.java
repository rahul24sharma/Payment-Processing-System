package com.payment.service.repository;

import com.payment.service.entity.Payment;
import com.payment.service.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Custom repository for complex payment queries
 */
public interface CustomPaymentRepository {
    
    /**
     * Get payment statistics for merchant
     */
    PaymentStatistics getStatistics(UUID merchantId, Instant startDate, Instant endDate);
    
    /**
     * Find payments with fetch joins (avoid N+1)
     */
    List<Payment> findWithRefunds(UUID merchantId, int limit);
    
    /**
     * Search payments by multiple criteria
     */
    List<Payment> searchPayments(PaymentSearchCriteria criteria);
    
    /**
     * Statistics DTO
     */
    record PaymentStatistics(
        long totalCount,
        BigDecimal totalAmount,
        BigDecimal averageAmount,
        long authorizedCount,
        long capturedCount,
        long failedCount
    ) {}
    
    /**
     * Search criteria
     */
    record PaymentSearchCriteria(
        UUID merchantId,
        UUID customerId,
        PaymentStatus status,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Instant startDate,
        Instant endDate
    ) {}
}