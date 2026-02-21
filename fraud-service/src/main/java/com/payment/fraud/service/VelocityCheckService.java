package com.payment.fraud.service;

import com.payment.fraud.dto.FraudAssessmentRequest;
import com.payment.fraud.entity.VelocityCounter;
import com.payment.fraud.repository.VelocityCounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class VelocityCheckService {
    
    private final VelocityCounterRepository velocityCounterRepository;
    
    /**
     * Check velocity (rate limiting)
     * Returns score based on how many attempts in time window
     */
    public BigDecimal checkVelocity(FraudAssessmentRequest request) {
        log.debug("Checking velocity: paymentId={}", request.getPaymentId());
        
        BigDecimal score = BigDecimal.ZERO;
        
        // Check 1: Card velocity (same payment method in 1 hour)
        if (request.getPaymentMethodId() != null) {
            int cardAttempts = incrementAndGetCount(
                "card:" + request.getPaymentMethodId(), 
                Duration.ofHours(1)
            );
            
            if (cardAttempts > 5) {
                score = score.add(BigDecimal.valueOf(30));
                log.warn("High card velocity: {} attempts in 1 hour", cardAttempts);
            } else if (cardAttempts > 3) {
                score = score.add(BigDecimal.valueOf(15));
            }
        }
        
        // Check 2: Customer velocity (same customer in 1 hour)
        if (request.getCustomerId() != null) {
            int customerAttempts = incrementAndGetCount(
                "customer:" + request.getCustomerId(), 
                Duration.ofHours(1)
            );
            
            if (customerAttempts > 10) {
                score = score.add(BigDecimal.valueOf(25));
                log.warn("High customer velocity: {} attempts in 1 hour", customerAttempts);
            }
        }
        
        // Check 3: IP velocity (same IP in 24 hours)
        if (request.getIpAddress() != null) {
            int ipAttempts = incrementAndGetCount(
                "ip:" + request.getIpAddress(), 
                Duration.ofHours(24)
            );
            
            if (ipAttempts > 50) {
                score = score.add(BigDecimal.valueOf(40));
                log.warn("High IP velocity: {} attempts in 24 hours", ipAttempts);
            } else if (ipAttempts > 20) {
                score = score.add(BigDecimal.valueOf(20));
            }
        }
        
        log.debug("Velocity check completed: score={}", score);
        
        return score.min(BigDecimal.valueOf(100));
    }
    
    /**
     * Increment counter and return current count
     */
    private int incrementAndGetCount(String key, Duration window) {
        Instant now = Instant.now();
        Instant windowEnd = now.plus(window);
        
        Optional<VelocityCounter> existing = velocityCounterRepository.findById(key);
        
        if (existing.isPresent()) {
            VelocityCounter counter = existing.get();
            
            // Check if expired
            if (counter.isExpired()) {
                // Reset counter
                counter.setCounter(1);
                counter.setWindowStart(now);
                counter.setWindowEnd(windowEnd);
            } else {
                // Increment
                counter.increment();
            }
            
            velocityCounterRepository.save(counter);
            return counter.getCounter();
            
        } else {
            // Create new counter
            VelocityCounter counter = VelocityCounter.builder()
                .id(key)
                .counter(1)
                .windowStart(now)
                .windowEnd(windowEnd)
                .build();
            
            velocityCounterRepository.save(counter);
            return 1;
        }
    }
    
    /**
     * Cleanup expired counters (scheduled job)
     */
    @Transactional
    public void cleanupExpiredCounters() {
        Instant expiryDate = Instant.now().minus(Duration.ofDays(7));
        int deleted = velocityCounterRepository.deleteExpiredCounters(expiryDate);
        log.info("Cleaned up {} expired velocity counters", deleted);
    }
}