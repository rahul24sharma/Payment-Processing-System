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
    
    @Query(
        value = """
            SELECT *
            FROM webhook_endpoints w
            WHERE w.merchant_id = :merchantId
              AND w.is_active = true
              AND (:eventType = ANY(w.events) OR '*' = ANY(w.events))
            """,
        nativeQuery = true
    )
    List<WebhookEndpoint> findByMerchantIdAndEventType(
        @Param("merchantId") UUID merchantId,
        @Param("eventType") String eventType
    );
}
