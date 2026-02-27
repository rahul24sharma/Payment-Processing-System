package com.payment.ledger.service;

import com.payment.ledger.entity.ProcessedEventConsumption;
import com.payment.ledger.repository.ProcessedEventConsumptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConsumerIdempotencyService {

    private final ProcessedEventConsumptionRepository repository;

    @Transactional
    public boolean tryAcquire(String topic, Map<String, Object> event, int partition, long offset) {
        String eventId = resolveEventId(event);
        String eventKey = eventId != null && !eventId.isBlank()
            ? topic + ":eventId:" + eventId
            : topic + ":offset:" + partition + ":" + offset;

        if (repository.existsById(eventKey)) {
            return false;
        }

        repository.save(ProcessedEventConsumption.builder()
            .eventKey(eventKey)
            .topic(topic)
            .eventId(eventId)
            .partitionId(partition)
            .offsetValue(offset)
            .createdAt(Instant.now())
            .build());
        return true;
    }

    private String resolveEventId(Map<String, Object> event) {
        Object eventId = event.get("eventId");
        return eventId != null ? String.valueOf(eventId) : null;
    }
}
