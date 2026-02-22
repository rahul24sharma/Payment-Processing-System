package com.payment.settlement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "settlement_batches", indexes = {
    @Index(name = "idx_settlement_batches_date", columnList = "settlement_date"),
    @Index(name = "idx_settlement_batches_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_settlement_date", columnNames = "settlement_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementBatch {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "settlement_date", nullable = false, unique = true)
    private LocalDate settlementDate;
    
    @Column(name = "capture_date", nullable = false)
    private LocalDate captureDate; // T-2 date
    
    @Column(name = "total_payments", nullable = false)
    @Builder.Default
    private Integer totalPayments = 0;
    
    @Column(name = "total_payouts", nullable = false)
    @Builder.Default
    private Integer totalPayouts = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PROCESSING;
    
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Payout> payouts = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    
    public void addPayout(Payout payout) {
        payouts.add(payout);
        payout.setBatch(this);
        totalPayouts++;
    }
}