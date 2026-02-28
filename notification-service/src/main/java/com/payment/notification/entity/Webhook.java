package com.payment.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhooks", indexes = {
    @Index(name = "idx_webhooks_merchant", columnList = "merchant_id"),
    @Index(name = "idx_webhooks_endpoint", columnList = "endpoint_id"),
    @Index(name = "idx_webhooks_status", columnList = "status"),
    @Index(name = "idx_webhooks_created", columnList = "created_at"),
    @Index(name = "idx_webhooks_pending", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Webhook {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;
    
    @Column(name = "endpoint_id")
    private UUID endpointId;
    
    @Column(nullable = false, length = 500)
    private String url;
    
    @Column(name = "event_type", nullable = false)
    private String eventType;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "webhook_status")
    @Builder.Default
    private WebhookStatus status = WebhookStatus.PENDING;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;
    
    @Column(name = "last_response_code")
    private Integer lastResponseCode;
    
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "delivered_at")
    private Instant deliveredAt;
    
    @Column(name = "failed_at")
    private Instant failedAt;
    
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    
    public void markDelivered(int responseCode) {
        this.status = WebhookStatus.DELIVERED;
        this.lastResponseCode = responseCode;
        this.deliveredAt = Instant.now();
    }
    
    public void markFailed(int responseCode, String error) {
        this.status = WebhookStatus.FAILED;
        this.lastResponseCode = responseCode;
        this.lastError = error;
        this.failedAt = Instant.now();
    }
    
    public void incrementAttempts() {
        this.attempts++;
    }
}
