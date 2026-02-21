package com.payment.fraud.service;

import com.payment.fraud.dto.FraudAssessmentRequest;
import com.payment.fraud.entity.FraudRule;
import com.payment.fraud.repository.FraudRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class RuleEngineService {
    
    private final FraudRuleRepository fraudRuleRepository;
    
    /**
     * Evaluate all active fraud rules
     */
    public BigDecimal evaluateRules(FraudAssessmentRequest request) {
        log.debug("Evaluating fraud rules: paymentId={}", request.getPaymentId());
        
        List<FraudRule> activeRules = fraudRuleRepository.findActiveRulesByPriority();
        
        BigDecimal totalScore = BigDecimal.ZERO;
        List<String> triggeredRules = new ArrayList<>();
        
        for (FraudRule rule : activeRules) {
            boolean triggered = evaluateRule(rule, request);
            
            if (triggered) {
                triggeredRules.add(rule.getRuleName());
                
                if (rule.getScoreImpact() != null) {
                    totalScore = totalScore.add(BigDecimal.valueOf(rule.getScoreImpact()));
                }
                
                // If action is BLOCK, return max score immediately
                if ("BLOCK".equals(rule.getAction())) {
                    log.warn("BLOCK rule triggered: rule={}, paymentId={}", 
                        rule.getRuleName(), request.getPaymentId());
                    return BigDecimal.valueOf(100);
                }
            }
        }
        
        log.debug("Rule evaluation completed: score={}, triggeredRules={}", 
            totalScore, triggeredRules);
        
        return totalScore.min(BigDecimal.valueOf(100));
    }
    
    /**
     * Evaluate a single rule
     */
    private boolean evaluateRule(FraudRule rule, FraudAssessmentRequest request) {
        Map<String, Object> conditions = rule.getConditions();
        
        return switch (rule.getRuleType()) {
            case "AMOUNT" -> evaluateAmountRule(conditions, request);
            case "VELOCITY" -> false; // Handled by VelocityCheckService
            case "GEOLOCATION" -> evaluateGeolocationRule(conditions, request);
            case "PATTERN" -> evaluatePatternRule(conditions, request);
            default -> {
                log.warn("Unknown rule type: {}", rule.getRuleType());
                yield false;
            }
        };
    }
    
    /**
     * Evaluate amount-based rule
     */
    private boolean evaluateAmountRule(Map<String, Object> conditions, 
                                      FraudAssessmentRequest request) {
        Object threshold = conditions.get("threshold");
        if (threshold instanceof Number) {
            long thresholdAmount = ((Number) threshold).longValue();
            return request.getAmount() > thresholdAmount;
        }
        return false;
    }
    
    /**
     * Evaluate geolocation rule (MVP stub)
     */
    private boolean evaluateGeolocationRule(Map<String, Object> conditions, 
                                           FraudAssessmentRequest request) {
        // TODO: Implement IP geolocation checking
        // For MVP, always return false
        return false;
    }
    
    /**
     * Evaluate pattern rule (MVP stub)
     */
    private boolean evaluatePatternRule(Map<String, Object> conditions, 
                                       FraudAssessmentRequest request) {
        // TODO: Implement pattern matching
        // For MVP, always return false
        return false;
    }
}