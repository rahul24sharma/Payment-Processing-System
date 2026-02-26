package com.payment.merchant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_ticket_comments", indexes = {
    @Index(name = "idx_support_ticket_comments_ticket_created", columnList = "ticket_id,created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SupportTicket ticket;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "author_type", nullable = false)
    private String authorType;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (authorType == null) {
            authorType = "MERCHANT";
        }
    }
}
