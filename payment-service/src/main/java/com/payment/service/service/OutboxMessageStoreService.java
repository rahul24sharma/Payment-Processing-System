package com.payment.service.service;

import com.payment.service.entity.OutboxMessage;
import com.payment.service.entity.OutboxMessageStatus;
import com.payment.service.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxMessageStoreService {

    private final OutboxMessageRepository outboxMessageRepository;

    @Transactional
    public List<OutboxMessage> claimBatch(int batchSize) {
        List<OutboxMessage> batch = outboxMessageRepository.claimableBatch(batchSize);
        Instant now = Instant.now();
        for (OutboxMessage message : batch) {
            message.setStatus(OutboxMessageStatus.PROCESSING);
            message.setLastError(null);
            message.setUpdatedAt(now);
        }
        return batch;
    }

    @Transactional
    public void markPublished(UUID outboxId) {
        outboxMessageRepository.findById(outboxId).ifPresent(message -> {
            message.setStatus(OutboxMessageStatus.PUBLISHED);
            message.setPublishedAt(Instant.now());
            message.setLastError(null);
            outboxMessageRepository.save(message);
        });
    }

    @Transactional
    public void markFailed(UUID outboxId, Exception ex, int maxAttempts) {
        outboxMessageRepository.findById(outboxId).ifPresent(message -> {
            int attempts = (message.getAttemptCount() == null ? 0 : message.getAttemptCount()) + 1;
            message.setAttemptCount(attempts);
            message.setStatus(OutboxMessageStatus.FAILED);
            if (attempts >= maxAttempts) {
                // Stop aggressive retries; keep FAILED and push availability far out.
                message.setAvailableAt(Instant.now().plusSeconds(3600));
            } else {
                message.setAvailableAt(Instant.now().plusSeconds(Math.min(60, attempts * 2L)));
            }
            String err = ex.getMessage();
            if (err != null && err.length() > 2000) {
                err = err.substring(0, 2000);
            }
            message.setLastError(err);
            outboxMessageRepository.save(message);
            log.error("Outbox publish failed: outboxId={}, attempts={}, eventType={}",
                outboxId, attempts, message.getEventType(), ex);
        });
    }
}
