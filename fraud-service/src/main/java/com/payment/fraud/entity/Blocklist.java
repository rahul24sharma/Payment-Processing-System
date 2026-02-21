package com.payment.fraud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blocklists", 
    indexes = {
        @Index(name = "idx_blocklists_type_value", columnList = "type, value")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "unique_blocklist_entry", columnNames = {"type", "value"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Blocklist {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String type; // EMAIL, CARD_BIN, IP, DEVICE_ID
    
    @Column(nullable = false)
    private String value;
    
    @Column(columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}