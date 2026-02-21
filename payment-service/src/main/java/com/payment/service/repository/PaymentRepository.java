package com.payment.service.repository;

import com.payment.service.entity.Payment;
import com.payment.service.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    /**
     * Find payment by idempotency key (for duplicate prevention)
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    
    /**
     * Find payment by ID with pessimistic locking (for updates)
     * Used during capture/refund to prevent concurrent modifications
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);
    
    /**
     * Find payments by merchant ID
     */
    List<Payment> findByMerchantId(UUID merchantId);
    
    /**
     * Find payments by merchant ID with pagination
     */
    Page<Payment> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId, Pageable pageable);
    
    /**
     * Find payments by merchant and status
     */
    @Query("""
        SELECT p FROM Payment p 
        WHERE p.merchantId = :merchantId 
        AND p.status IN :statuses 
        ORDER BY p.createdAt DESC
        """)
    List<Payment> findByMerchantAndStatuses(
        @Param("merchantId") UUID merchantId,
        @Param("statuses") List<PaymentStatus> statuses
    );
    
    /**
     * Find recent payments by merchant (for dashboard)
     */
    @Query("""
        SELECT p FROM Payment p 
        WHERE p.merchantId = :merchantId 
        AND p.createdAt >= :since
        ORDER BY p.createdAt DESC
        """)
    List<Payment> findRecentByMerchant(
        @Param("merchantId") UUID merchantId,
        @Param("since") Instant since
    );
    
    /**
     * Find payments by customer ID
     */
    List<Payment> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    
    /**
     * Find payments by status
     */
    List<Payment> findByStatus(PaymentStatus status);
    
    /**
     * Count payments by status (for metrics)
     */
    long countByStatus(PaymentStatus status);
    
    /**
     * Find payments captured between dates (for settlement)
     */
    @Query("""
        SELECT p FROM Payment p 
        WHERE p.capturedAt BETWEEN :start AND :end 
        AND p.status = 'CAPTURED'
        ORDER BY p.capturedAt
        """)
    List<Payment> findCapturedBetween(
        @Param("start") Instant start,
        @Param("end") Instant end
    );
    
    /**
     * Find authorized payments older than X days (to expire them)
     */
    @Query("""
        SELECT p FROM Payment p 
        WHERE p.status = 'AUTHORIZED' 
        AND p.authorizedAt < :expiryDate
        """)
    List<Payment> findExpiredAuthorizations(@Param("expiryDate") Instant expiryDate);
    
    /**
     * Check if payment exists by processor payment ID
     */
    boolean existsByProcessorPaymentId(String processorPaymentId);
    
    /**
     * Find payment by processor payment ID
     */
    Optional<Payment> findByProcessorPaymentId(String processorPaymentId);
    
    /**
     * Cursor-based pagination for efficient listing
     */
    @Query("""
        SELECT p FROM Payment p 
        WHERE p.merchantId = :merchantId
        AND (:cursor IS NULL OR p.createdAt < :cursor OR 
             (p.createdAt = :cursor AND p.id < :cursorId))
        AND (:status IS NULL OR p.status = :status)
        ORDER BY p.createdAt DESC, p.id DESC
        """)
    List<Payment> findWithCursor(
        @Param("merchantId") UUID merchantId,
        @Param("cursor") Instant cursor,
        @Param("cursorId") UUID cursorId,
        @Param("status") PaymentStatus status,
        Pageable pageable
    );
}