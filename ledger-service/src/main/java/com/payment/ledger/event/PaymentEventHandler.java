package com.payment.ledger.event;

import com.payment.ledger.service.LedgerService;
import com.payment.ledger.service.ConsumerIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ledger.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentEventHandler {
    
    private final LedgerService ledgerService;
    private final ConsumerIdempotencyService consumerIdempotencyService;
    
    /**
     * Listen to payment events from Kafka
     */
    @KafkaListener(
        topics = "payment-events",
        groupId = "ledger-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentEvent(
            @Payload Map<String, Object> event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        String eventType = (String) event.get("eventType");
        String paymentId = (String) event.get("paymentId");
        
        log.info("Received payment event: type={}, paymentId={}, partition={}, offset={}", 
            eventType, paymentId, partition, offset);

        if (!consumerIdempotencyService.tryAcquire("payment-events", event, partition, offset)) {
            log.info("Skipping duplicate payment event delivery: type={}, paymentId={}, partition={}, offset={}",
                eventType, paymentId, partition, offset);
            return;
        }
        
        try {
            switch (eventType) {
                case "PAYMENT_CAPTURED" -> handlePaymentCaptured(event);
                case "PAYMENT_REFUNDED" -> handlePaymentRefunded(event);
                default -> log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process payment event: eventType={}, paymentId={}", 
                eventType, paymentId, e);
            // In production, send to dead letter queue
            throw e; // Kafka will retry
        }
    }
    
    private void handlePaymentCaptured(Map<String, Object> event) {
        log.info("Processing PAYMENT_CAPTURED event");
        ledgerService.recordPaymentCapture(event);
    }
    
    private void handlePaymentRefunded(Map<String, Object> event) {
        log.info("Processing PAYMENT_REFUNDED event");
        ledgerService.recordRefund(event);
    }
}
