package com.payment.service.repository;

import com.payment.service.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    
    /**
     * Find all events for a payment (audit trail)
     */
    List<PaymentEvent> findByPaymentIdOrderByCreatedAtAsc(UUID paymentId);
    
    /**
     * Find events by type
     */
    List<PaymentEvent> findByEventTypeOrderByCreatedAtDesc(String eventType);
    
    /**
     * Find events created between dates
     */
    @Query("""
        SELECT pe FROM PaymentEvent pe 
        WHERE pe.createdAt BETWEEN :start AND :end
        ORDER BY pe.createdAt DESC
        """)
    List<PaymentEvent> findEventsBetween(
        @Param("start") Instant start,
        @Param("end") Instant end
    );
    
    /**
     * Count events by type
     */
    long countByEventType(String eventType);
}