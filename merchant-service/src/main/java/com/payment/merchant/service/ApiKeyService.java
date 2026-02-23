package com.payment.merchant.service;

import com.payment.merchant.entity.ApiKey;
import com.payment.merchant.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ApiKeyService {
    
    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Generate new API key for merchant
     */
    public ApiKeyWithPlaintext generateApiKey(UUID merchantId, String name, boolean isLive) {
        log.info("Generating API key: merchantId={}, name={}, isLive={}", 
            merchantId, name, isLive);
        
        // Generate random key
        String plainKey = generateSecureKey();
        String prefix = isLive ? "sk_live_" : "sk_test_";
        String fullKey = prefix + plainKey;
        
        // Hash the key
        String keyHash = passwordEncoder.encode(fullKey);
        
        // Save to database
        ApiKey apiKey = ApiKey.builder()
            .merchantId(merchantId)
            .keyHash(keyHash)
            .keyPrefix(prefix)
            .name(name)
            .isActive(true)
            .build();
        
        apiKey = apiKeyRepository.save(apiKey);
        
        log.info("API key generated: id={}, prefix={}", apiKey.getId(), prefix);
        
        // Return with plaintext key (only time it's shown!)
        return new ApiKeyWithPlaintext(apiKey, fullKey);
    }
    
    /**
     * List all API keys for a merchant
     */
    @Transactional(readOnly = true)
    public List<ApiKey> listApiKeys(UUID merchantId) {
        return apiKeyRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }
    
    /**
     * List only active API keys
     */
    @Transactional(readOnly = true)
    public List<ApiKey> listActiveApiKeys(UUID merchantId) {
        return apiKeyRepository.findByMerchantIdAndIsActiveTrueOrderByCreatedAtDesc(merchantId);
    }
    
    /**
     * Revoke an API key
     */
    public void revokeApiKey(UUID keyId, String revokedBy) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
            .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        
        apiKey.revoke(revokedBy);
        apiKeyRepository.save(apiKey);
        
        log.info("API key revoked: id={}, revokedBy={}", keyId, revokedBy);
    }
    
    /**
     * Validate API key
     */
    @Transactional(readOnly = true)
    public ApiKey validateApiKey(String providedKey) {
        // Find all active keys and check hash
        List<ApiKey> allKeys = apiKeyRepository.findAll();
        
        for (ApiKey apiKey : allKeys) {
            if (apiKey.getIsActive() && 
                !apiKey.isExpired() && 
                passwordEncoder.matches(providedKey, apiKey.getKeyHash())) {
                
                return apiKey;
            }
        }
        
        throw new IllegalArgumentException("Invalid API key");
    }
    
    /**
     * Update last used timestamp
     */
    @Transactional
    public void recordKeyUsage(UUID keyId) {
        apiKeyRepository.findById(keyId).ifPresent(apiKey -> {
            apiKey.setLastUsedAt(java.time.Instant.now());
            apiKeyRepository.save(apiKey);
        });
    }
    
    /**
     * Generate secure random key
     */
    private String generateSecureKey() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    /**
     * DTO to return API key with plaintext (only once!)
     */
    public record ApiKeyWithPlaintext(ApiKey apiKey, String plaintextKey) {}
}