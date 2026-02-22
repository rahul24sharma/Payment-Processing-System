package com.payment.notification.repository;

import com.payment.notification.entity.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {
    
    List<WebhookEndpoint> findByMerchantIdAndIsActiveTrue(UUID merchantId);
    
    @Query("""
        SELECT w FROM WebhookEndpoint w 
        WHERE w.merchantId = :merchantId 
        AND w.isActive = true 
        AND :eventType = ANY(w.events)
        """)
    List<WebhookEndpoint> findByMerchantIdAndEventType(
        @Param("merchantId") UUID merchantId,
        @Param("eventType") String eventType
    );
}