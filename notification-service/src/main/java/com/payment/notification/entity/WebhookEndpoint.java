package com.payment.notification.entity;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_endpoints", 
    indexes = {
        @Index(name = "idx_webhook_endpoints_merchant", columnList = "merchant_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "unique_merchant_url", columnNames = {"merchant_id", "url"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEndpoint {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;
    
    @Column(nullable = false, length = 500)
    private String url;
    
    @Column(nullable = false)
    private String secret; // For HMAC signing
    
    @Type(StringArrayType.class)
    @Column(name = "events", nullable = false, columnDefinition = "text[]")
    private String[] events; // Array of event types to subscribe to
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    public boolean subscribesTo(String eventType) {
        if (events == null || events.length == 0) {
            return false;
        }
        
        for (String event : events) {
            if (event.equals(eventType) || event.equals("*")) {
                return true;
            }
        }
        
        return false;
    }
}