package com.payment.fraud.service;

import com.payment.fraud.dto.FraudAssessmentRequest;
import com.payment.fraud.dto.FraudAssessmentResponse;
import com.payment.fraud.entity.FraudScore;
import com.payment.fraud.entity.RiskLevel;
import com.payment.fraud.repository.FraudScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class FraudAssessmentService {
    
    private final FraudScoreRepository fraudScoreRepository;
    private final VelocityCheckService velocityCheckService;
    private final RuleEngineService ruleEngineService;
    private final BlocklistService blocklistService;
    
    private static final String MODEL_VERSION = "1.0.0-mvp";
    
    /**
     * Main fraud assessment method
     * Runs multiple checks in parallel and combines scores
     */
    public FraudAssessmentResponse assess(FraudAssessmentRequest request) {
        log.info("Starting fraud assessment: paymentId={}, amount={}", 
            request.getPaymentId(), request.getAmount());
        
        // 1. Check blocklist first (fastest, most definitive)
        if (blocklistService.isBlocked(request)) {
            return createBlockedResponse(request, "Entity is blocklisted");
        }
        
        // 2. Run fraud checks in parallel for performance
        CompletableFuture<BigDecimal> velocityFuture = CompletableFuture
            .supplyAsync(() -> velocityCheckService.checkVelocity(request));
        
        CompletableFuture<BigDecimal> ruleFuture = CompletableFuture
            .supplyAsync(() -> ruleEngineService.evaluateRules(request));
        
        CompletableFuture<BigDecimal> mlFuture = CompletableFuture
            .supplyAsync(() -> calculateMLScore(request));
        
        // 3. Wait for all checks to complete
        CompletableFuture.allOf(velocityFuture, ruleFuture, mlFuture).join();
        
        BigDecimal velocityScore = velocityFuture.join();
        BigDecimal ruleScore = ruleFuture.join();
        BigDecimal mlScore = mlFuture.join();
        
        // 4. Calculate weighted final score
        BigDecimal finalScore = calculateWeightedScore(velocityScore, ruleScore, mlScore);
        
        // 5. Determine risk level and decision
        RiskLevel riskLevel = determineRiskLevel(finalScore);
        String decision = determineDecision(finalScore, riskLevel);
        
        // 6. Build factors map
        Map<String, Object> factors = buildFactors(velocityScore, ruleScore, mlScore, request);
        
        // 7. Save fraud score
        FraudScore fraudScore = FraudScore.builder()
            .paymentId(request.getPaymentId())
            .score(finalScore)
            .riskLevel(riskLevel)
            .velocityScore(velocityScore)
            .ruleScore(ruleScore)
            .mlScore(mlScore)
            .factors(factors)
            .modelVersion(MODEL_VERSION)
            .decision(decision)
            .build();
        
        fraudScoreRepository.save(fraudScore);
        
        log.info("Fraud assessment completed: paymentId={}, score={}, riskLevel={}, decision={}", 
            request.getPaymentId(), finalScore, riskLevel, decision);
        
        // 8. Return response
        return FraudAssessmentResponse.builder()
            .score(finalScore)
            .riskLevel(riskLevel.name().toLowerCase())
            .decision(decision)
            .factors(factors)
            .modelVersion(MODEL_VERSION)
            .build();
    }
    
    /**
     * Calculate weighted fraud score
     * Velocity: 30%, Rules: 40%, ML: 30%
     */
    private BigDecimal calculateWeightedScore(
            BigDecimal velocityScore, 
            BigDecimal ruleScore, 
            BigDecimal mlScore) {
        
        BigDecimal weighted = velocityScore.multiply(BigDecimal.valueOf(0.30))
            .add(ruleScore.multiply(BigDecimal.valueOf(0.40)))
            .add(mlScore.multiply(BigDecimal.valueOf(0.30)));
        
        // Cap at 100
        return weighted.min(BigDecimal.valueOf(100));
    }
    
    /**
     * ML-based fraud prediction (simplified for MVP)
     */
    private BigDecimal calculateMLScore(FraudAssessmentRequest request) {
        log.debug("Calculating ML score for payment: {}", request.getPaymentId());
        
        BigDecimal score = BigDecimal.ZERO;
        
        // Feature 1: Amount-based scoring
        if (request.getAmount() > 100000) { // > $1000
            score = score.add(BigDecimal.valueOf(20));
        }
        
        if (request.getAmount() > 500000) { // > $5000
            score = score.add(BigDecimal.valueOf(30));
        }
        
        // Feature 2: Round amounts are suspicious
        if (request.getAmount() % 10000 == 0) { // Exact $100, $200, etc.
            score = score.add(BigDecimal.valueOf(10));
        }
        
        // TODO: In production, call actual ML model (TensorFlow, PyTorch, etc.)
        
        return score.min(BigDecimal.valueOf(100));
    }
    
    /**
     * Determine risk level from score
     */
    private RiskLevel determineRiskLevel(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return RiskLevel.CRITICAL;
        } else if (score.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return RiskLevel.HIGH;
        } else if (score.compareTo(BigDecimal.valueOf(40)) >= 0) {
            return RiskLevel.MEDIUM;
        } else if (score.compareTo(BigDecimal.valueOf(20)) >= 0) {
            return RiskLevel.LOW;
        } else {
            return RiskLevel.VERY_LOW;
        }
    }
    
    /**
     * Determine action to take
     */
    private String determineDecision(BigDecimal score, RiskLevel riskLevel) {
        if (score.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return "BLOCK";
        } else if (score.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return "REVIEW";
        } else {
            return "ALLOW";
        }
    }
    
    /**
     * Build factors map explaining the score
     */
    private Map<String, Object> buildFactors(
            BigDecimal velocityScore,
            BigDecimal ruleScore,
            BigDecimal mlScore,
            FraudAssessmentRequest request) {
        
        Map<String, Object> factors = new HashMap<>();
        factors.put("velocity_score", velocityScore);
        factors.put("rule_score", ruleScore);
        factors.put("ml_score", mlScore);
        factors.put("amount", request.getAmount());
        factors.put("currency", request.getCurrency());
        
        // Add specific flags
        if (request.getAmount() > 100000) {
            factors.put("high_amount", true);
        }
        
        if (velocityScore.compareTo(BigDecimal.valueOf(30)) > 0) {
            factors.put("velocity_alert", true);
        }
        
        return factors;
    }
    
    /**
     * Create blocked response
     */
    private FraudAssessmentResponse createBlockedResponse(
            FraudAssessmentRequest request, 
            String reason) {
        
        log.warn("Payment blocked: paymentId={}, reason={}", request.getPaymentId(), reason);
        
        FraudScore fraudScore = FraudScore.builder()
            .paymentId(request.getPaymentId())
            .score(BigDecimal.valueOf(100))
            .riskLevel(RiskLevel.CRITICAL)
            .decision("BLOCK")
            .factors(Map.of("blocklist_reason", reason))
            .modelVersion(MODEL_VERSION)
            .build();
        
        fraudScoreRepository.save(fraudScore);
        
        return FraudAssessmentResponse.builder()
            .score(BigDecimal.valueOf(100))
            .riskLevel("critical")
            .decision("BLOCK")
            .factors(Map.of("reason", reason))
            .modelVersion(MODEL_VERSION)
            .build();
    }
}
