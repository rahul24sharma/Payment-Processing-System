package com.payment.notification.service;

import com.payment.notification.entity.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDlqPublisher {

    private static final String DLQ_TOPIC = "dead-letter-queue";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishWebhookFailure(Webhook webhook, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceService", "notification-service");
        payload.put("type", "webhook.delivery.failed");
        payload.put("timestamp", Instant.now().toString());
        payload.put("reason", reason);
        payload.put("webhookId", webhook.getId() != null ? webhook.getId().toString() : null);
        payload.put("merchantId", webhook.getMerchantId() != null ? webhook.getMerchantId().toString() : null);
        payload.put("endpointId", webhook.getEndpointId() != null ? webhook.getEndpointId().toString() : null);
        payload.put("url", webhook.getUrl());
        payload.put("eventType", webhook.getEventType());
        payload.put("attempts", webhook.getAttempts());
        payload.put("lastResponseCode", webhook.getLastResponseCode());
        payload.put("lastError", webhook.getLastError());
        payload.put("failedAt", webhook.getFailedAt() != null ? webhook.getFailedAt().toString() : null);

        String key = webhook.getMerchantId() != null ? webhook.getMerchantId().toString() : "unknown";

        kafkaTemplate.send(DLQ_TOPIC, key, payload)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish webhook failure to DLQ: webhookId={}", webhook.getId(), ex);
                } else {
                    log.warn("Published webhook failure to DLQ: webhookId={}, topic={}, partition={}",
                        webhook.getId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition());
                }
            });
    }
}
