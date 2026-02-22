package com.payment.notification.repository;

import com.payment.notification.entity.Webhook;
import com.payment.notification.entity.WebhookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, UUID> {
    
    List<Webhook> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
    
    List<Webhook> findByStatus(WebhookStatus status);
    
    /**
     * Find webhooks that need retry
     */
    @Query("""
        SELECT w FROM Webhook w 
        WHERE w.status = 'PENDING' 
        AND w.attempts < 5
        AND (w.nextRetryAt IS NULL OR w.nextRetryAt <= :now)
        ORDER BY w.createdAt
        """)
    List<Webhook> findPendingWebhooksForRetry(@Param("now") Instant now);
    
    List<Webhook> findByEndpointIdOrderByCreatedAtDesc(UUID endpointId);
}