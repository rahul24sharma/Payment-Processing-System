package com.payment.service.repository;

import com.payment.service.entity.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, UUID> {

    @Query(value = """
        SELECT * FROM outbox_messages
        WHERE status IN ('PENDING', 'FAILED')
          AND available_at <= NOW()
        ORDER BY created_at ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxMessage> claimableBatch(@Param("batchSize") int batchSize);
}
