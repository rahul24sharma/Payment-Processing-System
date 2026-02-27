package com.payment.merchant.controller;

import com.payment.merchant.entity.ApiKey;
import com.payment.merchant.security.MerchantRole;
import com.payment.merchant.security.RoleAccessService;
import com.payment.merchant.service.AuditLogService;
import com.payment.merchant.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

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
    private final RoleAccessService roleAccessService;
    private final AuditLogService auditLogService;
    
    @Operation(summary = "List all API keys for merchant")
    @GetMapping
    public ResponseEntity<List<ApiKey>> listApiKeys(
            @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader) {
        roleAccessService.requireAny(MerchantRole.ADMIN, MerchantRole.DEVELOPER);
        
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
            @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader,
            HttpServletRequest httpRequest) {
        roleAccessService.requireAny(MerchantRole.ADMIN, MerchantRole.DEVELOPER);
        
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
        auditLogService.log(
            merchantId, "MERCHANT_USER", merchantId.toString(), roleAccessService.currentRole().name(),
            "API_KEY_GENERATED", "API_KEY", result.apiKey().getId().toString(), "SUCCESS",
            Map.of("name", name, "isLive", isLive, "prefix", result.apiKey().getKeyPrefix()),
            httpRequest
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "Revoke API key")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeApiKey(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader,
            HttpServletRequest httpRequest) {
        roleAccessService.requireAny(MerchantRole.ADMIN, MerchantRole.DEVELOPER);
        
        String revokedBy = merchantIdHeader != null ? merchantIdHeader : "SYSTEM";
        
        apiKeyService.revokeApiKey(id, revokedBy);
        UUID merchantId = merchantIdHeader != null ? UUID.fromString(merchantIdHeader) : null;
        auditLogService.log(
            merchantId, "MERCHANT_USER", revokedBy, roleAccessService.currentRole().name(),
            "API_KEY_REVOKED", "API_KEY", id.toString(), "SUCCESS",
            null, httpRequest
        );
        
        return ResponseEntity.noContent().build();
    }
}
