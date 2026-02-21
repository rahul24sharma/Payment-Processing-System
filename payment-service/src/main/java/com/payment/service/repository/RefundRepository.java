package com.payment.service.repository;

import com.payment.service.entity.Refund;
import com.payment.service.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {
    
    /**
     * Find all refunds for a payment
     */
    List<Refund> findByPaymentId(UUID paymentId);
    
    /**
     * Find refunds by status
     */
    List<Refund> findByStatus(RefundStatus status);
    
    /**
     * Find successful refunds for a payment
     */
    @Query("""
        SELECT r FROM Refund r 
        WHERE r.payment.id = :paymentId 
        AND r.status = 'SUCCEEDED'
        ORDER BY r.createdAt DESC
        """)
    List<Refund> findSuccessfulRefundsByPayment(@Param("paymentId") UUID paymentId);
    
    /**
     * Find refund by processor refund ID
     */
    Optional<Refund> findByProcessorRefundId(String processorRefundId);
    
    /**
     * Find refunds created between dates
     */
    @Query("""
        SELECT r FROM Refund r 
        WHERE r.createdAt BETWEEN :start AND :end
        ORDER BY r.createdAt DESC
        """)
    List<Refund> findCreatedBetween(
        @Param("start") Instant start,
        @Param("end") Instant end
    );
    
    /**
     * Count refunds by payment
     */
    long countByPaymentId(UUID paymentId);
}