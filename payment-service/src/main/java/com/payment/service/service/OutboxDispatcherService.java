package com.payment.service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.service.entity.OutboxMessage;
import com.payment.service.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxDispatcherService {

    private final OutboxMessageStoreService outboxMessageStoreService;
    private final ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplateProvider;
    private final ObjectMapper objectMapper;

    @Value("${payment.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Value("${payment.outbox.dispatch.batch-size:50}")
    private int batchSize;

    @Value("${payment.outbox.dispatch.max-attempts:10}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${payment.outbox.dispatch.fixed-delay-ms:2000}")
    public void dispatchOutboxMessages() {
        if (!kafkaEnabled) {
            return;
        }
        KafkaTemplate<String, Object> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            return;
        }

        List<OutboxMessage> batch = outboxMessageStoreService.claimBatch(batchSize);
        if (batch.isEmpty()) {
            return;
        }

        for (OutboxMessage message : batch) {
            try {
                PaymentEvent event = objectMapper.readValue(message.getPayloadJson(), PaymentEvent.class);
                CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(message.getTopic(), message.getMessageKey(), event);

                SendResult<String, Object> result = future.get();
                outboxMessageStoreService.markPublished(message.getId());
                log.info("Published outbox event: outboxId={}, topic={}, partition={}, offset={}",
                    message.getId(),
                    message.getTopic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            } catch (Exception ex) {
                outboxMessageStoreService.markFailed(message.getId(), ex, maxAttempts);
            }
        }
    }
}
