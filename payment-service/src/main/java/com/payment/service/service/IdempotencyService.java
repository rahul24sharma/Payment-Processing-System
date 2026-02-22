package com.payment.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.service.entity.Payment;
import com.payment.service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {
    
    private final PaymentRepository paymentRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);
    
    /**
     * Find payment by idempotency key (Redis first, then DB)
     */
    public Optional<Payment> findByKey(String idempotencyKey) {
        String key = KEY_PREFIX + idempotencyKey;
        
        // Check Redis cache first
        String cached = redisTemplate.opsForValue().get(key);
        
        if (cached != null) {
            try {
                Payment payment = objectMapper.readValue(cached, Payment.class);
                log.info("Idempotency cache hit (Redis): key={}", idempotencyKey);
                return Optional.of(payment);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize cached payment", e);
                redisTemplate.delete(key);
            }
        }
        
        // Check database
        Optional<Payment> fromDb = paymentRepository.findByIdempotencyKey(idempotencyKey);
        
        // Cache in Redis if found
        fromDb.ifPresent(payment -> {
            try {
                String json = objectMapper.writeValueAsString(payment);
                redisTemplate.opsForValue().set(key, json, TTL);
                log.info("Cached payment in Redis: key={}", idempotencyKey);
            } catch (JsonProcessingException e) {
                log.error("Failed to cache payment in Redis", e);
            }
        });
        
        return fromDb;
    }
    
    /**
     * Store payment with idempotency key in Redis
     */
    public void store(String idempotencyKey, Payment payment) {
        String key = KEY_PREFIX + idempotencyKey;
        
        try {
            String json = objectMapper.writeValueAsString(payment);
            redisTemplate.opsForValue().set(key, json, TTL);
            log.info("Stored payment in Redis: key={}, paymentId={}", 
                idempotencyKey, payment.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to store payment in Redis", e);
            // Non-fatal - idempotency still works via database
        }
    }
}