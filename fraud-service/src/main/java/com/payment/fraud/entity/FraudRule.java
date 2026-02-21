package com.payment.fraud.entity;

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

@Entity
@Table(name = "fraud_rules", indexes = {
    @Index(name = "idx_fraud_rules_type", columnList = "rule_type"),
    @Index(name = "idx_fraud_rules_priority", columnList = "priority")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;
    
    @Column(name = "rule_type", nullable = false)
    private String ruleType; // VELOCITY, AMOUNT, GEOLOCATION, PATTERN
    
    @Type(JsonBinaryType.class)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> conditions;
    
    @Column(nullable = false)
    private String action; // BLOCK, REVIEW, ALLOW, SCORE
    
    @Column(name = "score_impact")
    private Integer scoreImpact;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 100;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
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
}