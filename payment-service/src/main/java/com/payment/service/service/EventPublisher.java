package com.payment.service.service;

import com.payment.service.entity.Payment;
import com.payment.service.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
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
                log.error("Failed to publish event: type={}, paymentId={}", 
                    eventType, payment.getId(), ex);
            }
        });
    }
}