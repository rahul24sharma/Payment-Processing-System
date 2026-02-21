package com.payment.service.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Audit trail for payment state changes
 */
@Entity
@Table(name = "payment_events", indexes = {
    @Index(name = "idx_payment_events_payment", columnList = "payment_id"),
    @Index(name = "idx_payment_events_type", columnList = "event_type"),
    @Index(name = "idx_payment_events_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
    
    @Column(name = "event_type", nullable = false)
    private String eventType; // CREATED, AUTHORIZED, CAPTURED, REFUNDED, FAILED
    
    @Column(name = "previous_state")
    private String previousState;
    
    @Column(name = "new_state")
    private String newState;
    
    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "created_by")
    private String createdBy; // user_id or "SYSTEM"
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}