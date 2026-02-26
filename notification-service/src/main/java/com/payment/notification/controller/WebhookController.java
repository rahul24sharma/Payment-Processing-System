package com.payment.notification.controller;

import com.payment.notification.entity.Webhook;
import com.payment.notification.entity.WebhookEndpoint;
import com.payment.notification.repository.WebhookRepository;
import com.payment.notification.service.WebhookEndpointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Webhook configuration and management")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {
    
    private final WebhookEndpointService endpointService;
    private final WebhookRepository webhookRepository;
    
    @Operation(summary = "Create webhook endpoint")
    @PostMapping("/endpoints")
    public ResponseEntity<WebhookEndpoint> createEndpoint(
            @RequestBody Map<String, Object> request,
            @RequestAttribute(value = "merchantId", required = false) UUID merchantIdAttr,
            @RequestHeader(value = "X-Merchant-Id", required = false) String merchantIdHeader) {
        UUID merchantId = resolveMerchantId(merchantIdAttr, merchantIdHeader);
        
        String url = (String) request.get("url");
        @SuppressWarnings("unchecked")
        List<String> eventsList = (List<String>) request.get("events");
        String[] events = eventsList.toArray(new String[0]);
        
        WebhookEndpoint endpoint = endpointService.createEndpoint(merchantId, url, events);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(endpoint);
    }

    /**
     * Alias endpoint to match planned API shape: POST /api/v1/webhooks
     */
    @Operation(summary = "Create webhook endpoint (alias)")
    @PostMapping
    public ResponseEntity<WebhookEndpoint> createWebhook(
            @RequestBody Map<String, Object> request,
            @RequestAttribute(value = "merchantId", required = false) UUID merchantIdAttr,
            @RequestHeader(value = "X-Merchant-Id", required = false) String merchantIdHeader) {
        return createEndpoint(request, merchantIdAttr, merchantIdHeader);
    }
    
    @Operation(summary = "List webhook endpoints")
    @GetMapping("/endpoints")
    public ResponseEntity<List<WebhookEndpoint>> listEndpoints(
            @RequestAttribute(value = "merchantId", required = false) UUID merchantIdAttr,
            @RequestHeader(value = "X-Merchant-Id", required = false) String merchantIdHeader) {
        UUID merchantId = resolveMerchantId(merchantIdAttr, merchantIdHeader);
        
        List<WebhookEndpoint> endpoints = endpointService.getEndpoints(merchantId);
        return ResponseEntity.ok(endpoints);
    }
    
    @Operation(summary = "Delete webhook endpoint")
    @DeleteMapping("/endpoints/{id}")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable UUID id) {
        endpointService.deleteEndpoint(id);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "List webhook delivery logs")
    @GetMapping("/logs")
    public ResponseEntity<List<Webhook>> getWebhookLogs(
            @RequestAttribute(value = "merchantId", required = false) UUID merchantIdAttr,
            @RequestHeader(value = "X-Merchant-Id", required = false) String merchantIdHeader) {
        UUID merchantId = resolveMerchantId(merchantIdAttr, merchantIdHeader);
        
        List<Webhook> webhooks = webhookRepository
            .findByMerchantIdOrderByCreatedAtDesc(merchantId);
        
        return ResponseEntity.ok(webhooks);
    }

    /**
     * Alias endpoint to match planned API shape: GET /api/v1/webhooks/{id}/logs
     */
    @Operation(summary = "List webhook delivery logs by endpoint id")
    @GetMapping("/{id}/logs")
    public ResponseEntity<List<Webhook>> getWebhookLogsByEndpoint(@PathVariable UUID id) {
        List<Webhook> webhooks = webhookRepository.findByEndpointIdOrderByCreatedAtDesc(id);
        return ResponseEntity.ok(webhooks);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is UP");
    }

    private UUID resolveMerchantId(UUID merchantIdAttr, String merchantIdHeader) {
        if (merchantIdAttr != null) {
            return merchantIdAttr;
        }
        if (merchantIdHeader != null && !merchantIdHeader.isBlank()) {
            try {
                return UUID.fromString(merchantIdHeader);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid X-Merchant-Id header");
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing merchantId (request attribute or X-Merchant-Id header)");
    }
}
