package com.payment.fraud.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "fraud_scores", indexes = {
    @Index(name = "idx_fraud_scores_payment", columnList = "payment_id"),
    @Index(name = "idx_fraud_scores_risk", columnList = "risk_level"),
    @Index(name = "idx_fraud_scores_score", columnList = "score"),
    @Index(name = "idx_fraud_scores_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudScore {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;
    
    @Column(name = "velocity_score", precision = 5, scale = 2)
    private BigDecimal velocityScore;
    
    @Column(name = "rule_score", precision = 5, scale = 2)
    private BigDecimal ruleScore;
    
    @Column(name = "ml_score", precision = 5, scale = 2)
    private BigDecimal mlScore;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> factors;
    
    @Column(name = "model_version")
    private String modelVersion;
    
    @Column(nullable = false)
    private String decision; // ALLOW, REVIEW, BLOCK
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}