package com.payment.fraud.repository;

import com.payment.fraud.entity.FraudScore;
import com.payment.fraud.entity.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FraudScoreRepository extends JpaRepository<FraudScore, UUID> {
    
    Optional<FraudScore> findByPaymentId(UUID paymentId);
    
    List<FraudScore> findByRiskLevel(RiskLevel riskLevel);
    
    @Query("SELECT f FROM FraudScore f WHERE f.createdAt >= :since ORDER BY f.score DESC")
    List<FraudScore> findHighScoresSince(@Param("since") Instant since);
    
    @Query("SELECT COUNT(f) FROM FraudScore f WHERE f.decision = :decision AND f.createdAt >= :since")
    long countByDecisionSince(@Param("decision") String decision, @Param("since") Instant since);
}