package com.payment.service.service;

import com.payment.service.entity.Payment;
import com.payment.service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotency service to prevent duplicate payments
 * In production, this would use Redis
 * For MVP, we use in-memory cache + database
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {
    
    private final PaymentRepository paymentRepository;
    
    // In-memory cache for MVP (in production, use Redis)
    private final ConcurrentHashMap<String, Payment> cache = new ConcurrentHashMap<>();
    
    /**
     * Find payment by idempotency key
     */
    public Optional<Payment> findByKey(String idempotencyKey) {
        // Check memory cache first
        Payment cached = cache.get(idempotencyKey);
        if (cached != null) {
            log.debug("Idempotency cache hit: key={}", idempotencyKey);
            return Optional.of(cached);
        }
        
        // Check database
        Optional<Payment> fromDb = paymentRepository.findByIdempotencyKey(idempotencyKey);
        
        // Cache if found
        fromDb.ifPresent(payment -> {
            cache.put(idempotencyKey, payment);
            log.debug("Cached payment from database: key={}", idempotencyKey);
        });
        
        return fromDb;
    }
    
    /**
     * Store payment with idempotency key
     */
    public void store(String idempotencyKey, Payment payment) {
        cache.put(idempotencyKey, payment);
        log.debug("Stored payment in idempotency cache: key={}, paymentId={}", 
            idempotencyKey, payment.getId());
        
        // TODO: In production, also store in Redis with TTL (24 hours)
    }
    
    /**
     * Clear old entries (cleanup job)
     */
    public void cleanup() {
        // For MVP, clear all (in production, use TTL in Redis)
        int size = cache.size();
        cache.clear();
        log.info("Cleared idempotency cache: {} entries removed", size);
    }
}