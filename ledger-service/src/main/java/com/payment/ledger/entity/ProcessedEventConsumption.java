package com.payment.ledger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "processed_event_consumptions", indexes = {
    @Index(name = "idx_processed_event_consumptions_topic_created_at", columnList = "topic, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEventConsumption {

    @Id
    @Column(name = "event_key", nullable = false, length = 255)
    private String eventKey;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "partition_id")
    private Integer partitionId;

    @Column(name = "offset_value")
    private Long offsetValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
