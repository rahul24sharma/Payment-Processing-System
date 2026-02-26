package com.payment.service.service;

import com.payment.service.entity.Payment;
import com.payment.service.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventPublisher {
    
    private final ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider;
    @Value("${payment.kafka.enabled:false}")
    private boolean kafkaEnabled;
    
    /**
     * Publish payment event to Kafka
     */
    public void publishPaymentEvent(String eventType, Payment payment, String previousStatus) {
        PaymentEvent event = PaymentEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(eventType)
            .paymentId(payment.getId())
            .merchantId(payment.getMerchantId())
            .customerId(payment.getCustomerId())
            .amount(payment.getAmount().getAmountInCents())
            .currency(payment.getAmount().getCurrency())
            .status(payment.getStatus().name())
            .previousStatus(previousStatus)
            .fraudScore(payment.getFraudScore())
            .metadata(payment.getMetadata())
            .timestamp(Instant.now())
            .build();
        
        String topic = "payment-events";
        String key = payment.getId().toString();

        if (!kafkaEnabled) {
            log.warn("Kafka publishing disabled; skipping event publish: type={}, paymentId={}",
                eventType, payment.getId());
            return;
        }

        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();

        if (kafkaTemplate == null) {
            log.warn("Kafka is disabled/unavailable; skipping event publish: type={}, paymentId={}",
                eventType, payment.getId());
            return;
        }

        try {
            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published event: type={}, paymentId={}, partition={}, offset={}",
                        eventType,
                        payment.getId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish event asynchronously: type={}, paymentId={}",
                        eventType, payment.getId(), ex);
                }
            });
        } catch (Exception ex) {
            // Local/dev environments may not have Kafka or topics provisioned.
            // Publishing is best-effort and must not fail payment API requests.
            log.error("Failed to publish event synchronously: type={}, paymentId={}",
                eventType, payment.getId(), ex);
        }
    }
}
