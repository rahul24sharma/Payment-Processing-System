package com.payment.service.service;

import com.payment.service.entity.Money;
import com.payment.service.entity.Payment;
import com.payment.service.exception.ProcessorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

/**
 * Mock payment processor for MVP
 * Simulates calls to Stripe, Braintree, etc.
 * 
 * In production, this would be replaced with actual processor integration
 */
@Service
@Slf4j
public class MockProcessorService {
    
    private final Random random = new Random();
    
    /**
     * Authorize a payment
     * Returns processor payment ID
     */
    public String authorize(Payment payment) {
        log.info("Mock processor: Authorizing payment - amount={}, currency={}", 
            payment.getAmount(), payment.getAmount().getCurrency());
        
        // Simulate network delay
        simulateDelay(100, 300);
        
        // Simulate 5% failure rate
        if (random.nextInt(100) < 5) {
            log.warn("Mock processor: Authorization declined");
            throw new ProcessorException(
                "Card declined by issuer",
                "mock_processor",
                "card_declined"
            );
        }
        
        // Generate mock processor payment ID
        String processorPaymentId = "proc_" + UUID.randomUUID().toString().substring(0, 8);
        
        log.info("Mock processor: Authorization successful - processorId={}", 
            processorPaymentId);
        
        return processorPaymentId;
    }
    
    /**
     * Capture an authorized payment
     */
    public void capture(String processorPaymentId, Money amount) {
        log.info("Mock processor: Capturing payment - processorId={}, amount={}", 
            processorPaymentId, amount);
        
        simulateDelay(100, 300);
        
        // Simulate 2% failure rate
        if (random.nextInt(100) < 2) {
            log.warn("Mock processor: Capture failed");
            throw new ProcessorException(
                "Capture failed - insufficient funds",
                "mock_processor",
                "insufficient_funds"
            );
        }
        
        log.info("Mock processor: Capture successful - processorId={}", processorPaymentId);
    }
    
    /**
     * Void an authorization
     */
    public void voidAuthorization(String processorPaymentId) {
        log.info("Mock processor: Voiding authorization - processorId={}", processorPaymentId);
        
        simulateDelay(50, 150);
        
        log.info("Mock processor: Void successful - processorId={}", processorPaymentId);
    }
    
    /**
     * Process a refund
     */
    public String refund(String processorPaymentId, Money amount) {
        log.info("Mock processor: Processing refund - processorId={}, amount={}", 
            processorPaymentId, amount);
        
        simulateDelay(100, 300);
        
        // Simulate 1% failure rate
        if (random.nextInt(100) < 1) {
            log.warn("Mock processor: Refund failed");
            throw new ProcessorException(
                "Refund failed - payment already refunded",
                "mock_processor",
                "already_refunded"
            );
        }
        
        String refundId = "ref_" + UUID.randomUUID().toString().substring(0, 8);
        
        log.info("Mock processor: Refund successful - refundId={}", refundId);
        
        return refundId;
    }
    
    /**
     * Simulate network delay
     */
    private void simulateDelay(int minMs, int maxMs) {
        try {
            int delay = random.nextInt(maxMs - minMs) + minMs;
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Health check
     */
    public boolean healthCheck() {
        return true; // Mock processor is always "healthy"
    }
}