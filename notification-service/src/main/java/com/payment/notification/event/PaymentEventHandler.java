package com.payment.notification.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.notification.entity.Webhook;
import com.payment.notification.entity.WebhookEndpoint;
import com.payment.notification.repository.WebhookEndpointRepository;
import com.payment.notification.repository.WebhookRepository;
import com.payment.notification.service.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventHandler {
    
    private final WebhookEndpointRepository endpointRepository;
    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryService webhookDeliveryService;
    private final ObjectMapper objectMapper;
    
    /**
     * Listen to payment events and send webhooks
     */
    @KafkaListener(
        topics = "payment-events",
        groupId = "notification-service-group"
    )
    public void handlePaymentEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        String eventType = (String) event.get("eventType");
        String merchantIdStr = (String) event.get("merchantId");
        
        log.info("Received payment event for notifications: type={}, merchantId={}, partition={}, offset={}", 
            eventType, merchantIdStr, partition, offset);
        
        try {
            UUID merchantId = UUID.fromString(merchantIdStr);
            
            // Find webhook endpoints subscribed to this event type
            List<WebhookEndpoint> endpoints = endpointRepository
                .findByMerchantIdAndEventType(merchantId, eventType);
            
            if (endpoints.isEmpty()) {
                log.debug("No webhook endpoints configured for merchant: {} and event: {}", 
                    merchantId, eventType);
                return;
            }
            
            // Create webhook for each endpoint
            for (WebhookEndpoint endpoint : endpoints) {
                try {
                    Webhook webhook = createWebhook(endpoint, eventType, event);
                    webhookRepository.save(webhook);
                    
                    // Deliver asynchronously
                    webhookDeliveryService.deliverWebhook(webhook);
                    
                } catch (Exception e) {
                    log.error("Failed to create webhook for endpoint: {}", endpoint.getId(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to process payment event for notifications: {}", event, e);
            throw e; // Kafka will retry
        }
    }
    
    /**
     * Create webhook entity from event
     */
    private Webhook createWebhook(WebhookEndpoint endpoint, String eventType, Map<String, Object> event) 
            throws JsonProcessingException {
        
        // Build webhook payload
        Map<String, Object> webhookPayload = new HashMap<>();
        webhookPayload.put("id", "evt_" + UUID.randomUUID().toString().substring(0, 8));
        webhookPayload.put("type", eventType);
        webhookPayload.put("created_at", event.get("timestamp"));
        webhookPayload.put("data", event);
        
        String payload = objectMapper.writeValueAsString(webhookPayload);
        
        return Webhook.builder()
            .merchantId(endpoint.getMerchantId())
            .endpointId(endpoint.getId())
            .url(endpoint.getUrl())
            .eventType(eventType)
            .payload(payload)
            .build();
    }
}