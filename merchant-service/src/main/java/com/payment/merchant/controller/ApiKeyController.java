package com.payment.merchant.controller;

import com.payment.merchant.entity.ApiKey;
import com.payment.merchant.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/api-keys")
@Tag(name = "API Keys", description = "API key management")
@RequiredArgsConstructor
@Slf4j
public class ApiKeyController {
    
    private final ApiKeyService apiKeyService;
    
    @Operation(summary = "List all API keys for merchant")
    @GetMapping
    public ResponseEntity<List<ApiKey>> listApiKeys(
            @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader) {
        
        UUID merchantId = merchantIdHeader != null 
            ? UUID.fromString(merchantIdHeader) 
            : UUID.randomUUID();
        
        List<ApiKey> apiKeys = apiKeyService.listApiKeys(merchantId);
        
        // Don't return the hash
        apiKeys.forEach(key -> key.setKeyHash("***REDACTED***"));
        
        return ResponseEntity.ok(apiKeys);
    }
    
    @Operation(summary = "Generate new API key")
    @PostMapping
    public ResponseEntity<Map<String, Object>> generateApiKey(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader) {
        
        UUID merchantId = merchantIdHeader != null 
            ? UUID.fromString(merchantIdHeader) 
            : UUID.randomUUID();
        
        String name = (String) request.getOrDefault("name", "Unnamed Key");
        Boolean isLive = (Boolean) request.getOrDefault("isLive", false);
        
        ApiKeyService.ApiKeyWithPlaintext result = apiKeyService.generateApiKey(
            merchantId, name, isLive);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", result.apiKey().getId());
        response.put("name", result.apiKey().getName());
        response.put("key", result.plaintextKey()); // Only returned once!
        response.put("prefix", result.apiKey().getKeyPrefix());
        response.put("createdAt", result.apiKey().getCreatedAt());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "Revoke API key")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeApiKey(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader) {
        
        String revokedBy = merchantIdHeader != null ? merchantIdHeader : "SYSTEM";
        
        apiKeyService.revokeApiKey(id, revokedBy);
        
        return ResponseEntity.noContent().build();
    }
}