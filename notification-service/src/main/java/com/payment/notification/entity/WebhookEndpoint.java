package com.payment.notification.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
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
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "webhook_endpoint_events",
        joinColumns = @JoinColumn(name = "endpoint_id")
    )
    @Column(name = "event_type", nullable = false)
    private List<String> events; // Event types to subscribe to
    
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
        if (events == null || events.isEmpty()) {
            return false;
        }

        return events.stream().anyMatch(event -> event.equals(eventType) || event.equals("*"));
    }
}
