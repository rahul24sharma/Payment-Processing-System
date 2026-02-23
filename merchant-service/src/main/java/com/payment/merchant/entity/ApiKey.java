package com.payment.merchant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys", indexes = {
    @Index(name = "idx_api_keys_merchant", columnList = "merchant_id"),
    @Index(name = "idx_api_keys_hash", columnList = "key_hash", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;
    
    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;
    
    @Column(name = "key_prefix", nullable = false)
    private String keyPrefix; // "sk_live_" or "sk_test_"
    
    @Column
    private String name; // User-defined name for the key
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "last_used_at")
    private Instant lastUsedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    @Column(name = "revoked_at")
    private Instant revokedAt;
    
    @Column(name = "revoked_by")
    private String revokedBy;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    
    public void revoke(String revokedBy) {
        this.isActive = false;
        this.revokedAt = Instant.now();
        this.revokedBy = revokedBy;
    }
    
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}