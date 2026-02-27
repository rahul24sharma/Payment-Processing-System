package com.payment.service.service;

import com.payment.service.entity.Payment;
import com.payment.service.entity.OutboxMessage;
import com.payment.service.entity.OutboxMessageStatus;
import com.payment.service.event.PaymentEvent;
import com.payment.service.repository.OutboxMessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventPublisher {

    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Publish payment event to Kafka
     */
    @Transactional
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

        String payloadJson = toJson(event);

        OutboxMessage message = OutboxMessage.builder()
            .id(UUID.randomUUID())
            .aggregateType("PAYMENT")
            .aggregateId(payment.getId())
            .eventType(eventType)
            .topic(PAYMENT_EVENTS_TOPIC)
            .messageKey(payment.getId().toString())
            .payloadJson(payloadJson)
            .status(OutboxMessageStatus.PENDING)
            .attemptCount(0)
            .availableAt(Instant.now())
            .build();

        outboxMessageRepository.save(message);
        log.info("Queued outbox event: type={}, paymentId={}, outboxId={}",
            eventType, payment.getId(), message.getId());
    }

    private String toJson(PaymentEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize payment event for outbox", e);
        }
    }
}
