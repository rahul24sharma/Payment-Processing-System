package com.payment.notification.service;

import com.payment.notification.entity.WebhookEndpoint;
import com.payment.notification.repository.WebhookEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class WebhookEndpointService {
    
    private final WebhookEndpointRepository endpointRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Create a new webhook endpoint
     */
    public WebhookEndpoint createEndpoint(UUID merchantId, String url, String[] events) {
        log.info("Creating webhook endpoint: merchantId={}, url={}", merchantId, url);
        
        // Generate webhook secret
        String secret = generateWebhookSecret();
        
        WebhookEndpoint endpoint = WebhookEndpoint.builder()
            .merchantId(merchantId)
            .url(url)
            .secret(secret)
            .events(events == null ? List.of("*") : Arrays.asList(events))
            .isActive(true)
            .build();
        
        endpoint = endpointRepository.save(endpoint);
        
        log.info("Webhook endpoint created: id={}", endpoint.getId());
        
        return endpoint;
    }
    
    /**
     * Get all endpoints for a merchant
     */
    @Transactional(readOnly = true)
    public List<WebhookEndpoint> getEndpoints(UUID merchantId) {
        return endpointRepository.findByMerchantIdAndIsActiveTrue(merchantId);
    }
    
    /**
     * Delete (deactivate) webhook endpoint
     */
    public void deleteEndpoint(UUID endpointId) {
        endpointRepository.findById(endpointId).ifPresent(endpoint -> {
            endpoint.setIsActive(false);
            endpointRepository.save(endpoint);
            log.info("Webhook endpoint deactivated: id={}", endpointId);
        });
    }
    
    /**
     * Generate secure webhook secret
     */
    private String generateWebhookSecret() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return "whsec_" + Base64.getUrlEncoder().withoutPadding()
            .encodeToString(randomBytes);
    }
}
