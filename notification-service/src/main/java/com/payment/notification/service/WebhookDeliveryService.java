package com.payment.notification.service;

import com.payment.notification.entity.Webhook;
import com.payment.notification.repository.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookDeliveryService {
    
    private final WebhookRepository webhookRepository;
    private final RestTemplate restTemplate;
    private final NotificationDlqPublisher notificationDlqPublisher;
    
    private static final int MAX_RETRIES = 5;
    private static final int[] RETRY_DELAYS = {60, 300, 900, 3600, 7200}; // seconds: 1min, 5min, 15min, 1hr, 2hr
    
    /**
     * Deliver webhook asynchronously with retry logic
     */
    @Async("webhookExecutor")
    @Transactional
    public CompletableFuture<Void> deliverWebhook(Webhook webhook) {
        log.info("Delivering webhook: id={}, url={}, attempt={}", 
            webhook.getId(), webhook.getUrl(), webhook.getAttempts() + 1);
        
        webhook.incrementAttempts();
        
        try {
            // Generate HMAC signature
            String signature = generateSignature(webhook);
            
            // Build request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-ID", webhook.getId().toString());
            headers.set("X-Webhook-Signature", signature);
            headers.set("X-Webhook-Timestamp", String.valueOf(Instant.now().getEpochSecond()));
            headers.set("X-Webhook-Event", webhook.getEventType());
            headers.set("X-Webhook-Attempt", String.valueOf(webhook.getAttempts()));
            headers.set("User-Agent", "PaymentSystem-Webhook/1.0");
            
            HttpEntity<String> request = new HttpEntity<>(webhook.getPayload(), headers);
            
            // Send webhook with 10 second timeout
            ResponseEntity<String> response = restTemplate.exchange(
                webhook.getUrl(),
                HttpMethod.POST,
                request,
                String.class
            );
            
            // Check if successful (2xx)
            if (response.getStatusCode().is2xxSuccessful()) {
                webhook.markDelivered(response.getStatusCode().value());
                webhookRepository.save(webhook);
                
                log.info("Webhook delivered successfully: id={}, responseCode={}", 
                    webhook.getId(), response.getStatusCode().value());
                
                return CompletableFuture.completedFuture(null);
            } else {
                log.warn("Webhook delivery received non-2xx response: id={}, code={}", 
                    webhook.getId(), response.getStatusCode().value());
                
                scheduleRetry(webhook, response.getStatusCode().value(), 
                    "Non-2xx response: " + response.getStatusCode());
            }
            
        } catch (ResourceAccessException e) {
            // Timeout or connection error
            log.warn("Webhook delivery failed (timeout/connection): id={}, attempt={}", 
                webhook.getId(), webhook.getAttempts(), e);
            
            scheduleRetry(webhook, null, "Connection timeout: " + e.getMessage());
            
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // HTTP error
            log.warn("Webhook delivery failed with HTTP error: id={}, code={}", 
                webhook.getId(), e.getStatusCode());
            
            // Don't retry 4xx errors (except 408 Request Timeout, 429 Too Many Requests)
            if (e.getStatusCode().is4xxClientError() && 
                e.getStatusCode().value() != 408 && 
                e.getStatusCode().value() != 429) {
                
                webhook.markFailed(e.getStatusCode().value(), 
                    "4xx client error: " + e.getStatusText());
                webhookRepository.save(webhook);
                
                log.error("Webhook delivery failed permanently (4xx): id={}", webhook.getId());
                return CompletableFuture.completedFuture(null);
            }
            
            scheduleRetry(webhook, e.getStatusCode().value(), e.getStatusText());
            
        } catch (Exception e) {
            log.error("Unexpected error delivering webhook: id={}", webhook.getId(), e);
            scheduleRetry(webhook, null, "Unexpected error: " + e.getMessage());
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Schedule retry or mark as permanently failed
     */
    private void scheduleRetry(Webhook webhook, Integer responseCode, String error) {
        webhook.setLastResponseCode(responseCode);
        webhook.setLastError(error);
        
        if (webhook.getAttempts() >= MAX_RETRIES) {
            webhook.markFailed(responseCode != null ? responseCode : 0, 
                "Failed after " + MAX_RETRIES + " attempts: " + error);
            
            log.error("Webhook delivery failed permanently: id={}, attempts={}", 
                webhook.getId(), webhook.getAttempts());

            // Publish terminal failures to a dead-letter topic for ops investigation/replay tooling.
            notificationDlqPublisher.publishWebhookFailure(webhook, error);
        } else {
            // Schedule next retry
            int delaySeconds = RETRY_DELAYS[webhook.getAttempts() - 1];
            webhook.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
            
            log.info("Webhook scheduled for retry: id={}, nextRetry={}s", 
                webhook.getId(), delaySeconds);
        }
        
        webhookRepository.save(webhook);
    }
    
    /**
     * Generate HMAC-SHA256 signature for webhook verification
     */
    private String generateSignature(Webhook webhook) {
        try {
            // Get webhook secret from endpoint
            // For MVP, using a default secret
            String secret = "webhook_secret_123"; // TODO: Get from WebhookEndpoint
            
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
            );
            hmac.init(secretKey);
            
            // Sign: webhook_id.timestamp.payload
            String signedPayload = webhook.getId() + "." + 
                Instant.now().getEpochSecond() + "." + 
                webhook.getPayload();
            
            byte[] hash = hmac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            
            return "sha256=" + bytesToHex(hash);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate webhook signature", e);
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
