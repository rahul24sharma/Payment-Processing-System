package com.payment.merchant.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "merchants", indexes = {
    @Index(name = "idx_merchants_email", columnList = "email", unique = true),
    @Index(name = "idx_merchants_api_key", columnList = "api_key_hash", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "business_name", nullable = false)
    private String businessName;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(name = "api_key_hash", unique = true)
    private String apiKeyHash;
    
    @Column(name = "api_key_prefix")
    private String apiKeyPrefix; // e.g., "sk_test_" or "sk_live_"
    
    @Column(name = "status")
    @ColumnTransformer(write = "?::merchant_status")
    private String status; // ACTIVE, SUSPENDED, PENDING_REVIEW

    @Column(name = "risk_profile")
    @ColumnTransformer(write = "?::risk_profile")
    private String riskProfile; // LOW, MEDIUM, HIGH
    
    @Type(JsonBinaryType.class)
    @Column(name = "settings", columnDefinition = "jsonb")
    private Map<String, Object> settings;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = "ACTIVE";
        }
        if (riskProfile == null) {
            riskProfile = "MEDIUM";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}