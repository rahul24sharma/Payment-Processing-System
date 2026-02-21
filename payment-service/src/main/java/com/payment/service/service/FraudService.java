package com.payment.service.service;

import com.payment.service.entity.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Fraud detection service for MVP
 * In full version, this would be a separate microservice
 */
@Service
@Slf4j
public class FraudService {
    
    /**
     * Assess fraud risk for a payment
     * Returns score from 0-100 (0 = safe, 100 = definitely fraud)
     */
    public BigDecimal assessRisk(Payment payment) {
        log.info("Assessing fraud risk: paymentId={}, amount={}", 
            payment.getId(), payment.getAmount());
        
        BigDecimal score = BigDecimal.ZERO;
        
        // Rule 1: High amount check
        if (payment.getAmount().getAmount().compareTo(BigDecimal.valueOf(1000)) > 0) {
            score = score.add(BigDecimal.valueOf(20));
            log.debug("High amount detected: +20 points");
        }
        
        // Rule 2: Very high amount
        if (payment.getAmount().getAmount().compareTo(BigDecimal.valueOf(5000)) > 0) {
            score = score.add(BigDecimal.valueOf(30));
            log.debug("Very high amount detected: +30 points");
        }
        
        // Rule 3: Check if amount is suspiciously round
        BigDecimal amount = payment.getAmount().getAmount();
        if (amount.remainder(BigDecimal.valueOf(100)).compareTo(BigDecimal.ZERO) == 0) {
            score = score.add(BigDecimal.valueOf(5));
            log.debug("Round amount detected: +5 points");
        }
        
        // TODO: Add more sophisticated rules:
        // - Velocity checks (multiple payments from same card in short time)
        // - Geolocation mismatch
        // - Device fingerprinting
        // - ML model prediction
        
        // Cap score at 100
        if (score.compareTo(BigDecimal.valueOf(100)) > 0) {
            score = BigDecimal.valueOf(100);
        }
        
        log.info("Fraud assessment completed: paymentId={}, score={}", 
            payment.getId(), score);
        
        return score;
    }
    
    /**
     * Check velocity (number of attempts)
     * For MVP, we'll keep it simple
     */
    private boolean checkVelocity(Payment payment) {
        // TODO: Implement velocity checking
        // Count payments from same customer in last hour
        // If > 5, return true (suspicious)
        return false;
    }
}