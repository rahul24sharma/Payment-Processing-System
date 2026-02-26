package com.payment.notification.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.notification.entity.Webhook;
import com.payment.notification.entity.WebhookEndpoint;
import com.payment.notification.repository.WebhookEndpointRepository;
import com.payment.notification.repository.WebhookRepository;
import com.payment.notification.service.EmailNotificationService;
import com.payment.notification.service.SmsNotificationService;
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
    private final EmailNotificationService emailNotificationService;
    private final SmsNotificationService smsNotificationService;
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
        
        processEvent("payment-events", event, partition, offset, true);
    }

    @KafkaListener(
        topics = "settlement-events",
        groupId = "notification-service-group"
    )
    public void handleSettlementEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        processEvent("settlement-events", event, partition, offset, false);
    }

    @KafkaListener(
        topics = "fraud-events",
        groupId = "notification-service-group"
    )
    public void handleFraudEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        processEvent("fraud-events", event, partition, offset, false);
    }

    private void processEvent(
            String topic,
            Map<String, Object> event,
            int partition,
            long offset,
            boolean triggerEmailAndSms) {

        String eventType = resolveEventType(event, topic);
        String merchantIdStr = resolveMerchantId(event);

        log.info("Received event for notifications: topic={}, type={}, merchantId={}, partition={}, offset={}",
            topic, eventType, merchantIdStr, partition, offset);

        if (merchantIdStr == null || merchantIdStr.isBlank()) {
            log.warn("Skipping notification event without merchantId: topic={}, payload={}", topic, event);
            return;
        }

        try {
            UUID merchantId = UUID.fromString(merchantIdStr);

            if (triggerEmailAndSms) {
                try {
                    emailNotificationService.sendPaymentEventEmail(event);
                } catch (Exception e) {
                    log.warn("Failed to enqueue payment email notification for eventType={}", eventType, e);
                }

                try {
                    smsNotificationService.sendPaymentEventSms(event);
                } catch (Exception e) {
                    log.warn("Failed to enqueue payment SMS notification for eventType={}", eventType, e);
                }
            }

            List<WebhookEndpoint> endpoints = endpointRepository
                .findByMerchantIdAndEventType(merchantId, eventType);

            if (endpoints.isEmpty()) {
                log.debug("No webhook endpoints configured for merchant: {} and event: {}",
                    merchantId, eventType);
                return;
            }

            for (WebhookEndpoint endpoint : endpoints) {
                try {
                    Webhook webhook = createWebhook(endpoint, eventType, event);
                    webhookRepository.save(webhook);
                    webhookDeliveryService.deliverWebhook(webhook);
                } catch (Exception e) {
                    log.error("Failed to create webhook for endpoint: {}", endpoint.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process event for notifications: topic={}, payload={}", topic, event, e);
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

    private String resolveEventType(Map<String, Object> event, String topic) {
        Object explicit = event.get("eventType");
        if (explicit instanceof String s && !s.isBlank()) {
            return s;
        }
        Object type = event.get("type");
        if (type instanceof String s && !s.isBlank()) {
            return s;
        }
        return switch (topic) {
            case "settlement-events" -> "settlement.completed";
            case "fraud-events" -> "fraud.alert";
            default -> "unknown.event";
        };
    }

    private String resolveMerchantId(Map<String, Object> event) {
        Object merchantId = event.get("merchantId");
        if (merchantId != null) {
            return String.valueOf(merchantId);
        }
        Object data = event.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object nestedMerchantId = dataMap.get("merchantId");
            if (nestedMerchantId != null) {
                return String.valueOf(nestedMerchantId);
            }
        }
        return null;
    }
}
