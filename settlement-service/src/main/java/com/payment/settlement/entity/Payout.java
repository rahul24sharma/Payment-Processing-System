package com.payment.settlement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payouts", indexes = {
    @Index(name = "idx_payouts_batch", columnList = "batch_id"),
    @Index(name = "idx_payouts_merchant", columnList = "merchant_id"),
    @Index(name = "idx_payouts_date", columnList = "settlement_date"),
    @Index(name = "idx_payouts_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payout {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private SettlementBatch batch;
    
    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;
    
    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;
    
    // Amounts
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount; // Total of all payments
    
    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal feeAmount; // Platform fees already deducted
    
    @Column(name = "net_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal netAmount; // After fees
    
    @Column(name = "reserve_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal reserveAmount; // Held for chargebacks (5%)
    
    @Column(name = "payout_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal payoutAmount; // Actually transferred
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Column(name = "payment_count", nullable = false)
    private Integer paymentCount;
    
    // Bank transfer details
    @Column(name = "bank_transfer_id")
    private String bankTransferId;
    
    @Column(name = "bank_account_id")
    private UUID bankAccountId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.PENDING;
    
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @Column(name = "failed_at")
    private Instant failedAt;
    
    @OneToMany(mappedBy = "payout", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PayoutPayment> payments = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    public void addPayment(PayoutPayment payoutPayment) {
        payments.add(payoutPayment);
        payoutPayment.setPayout(this);
    }
    
    public void markCompleted(String bankTransferId) {
        this.status = PayoutStatus.COMPLETED;
        this.bankTransferId = bankTransferId;
        this.completedAt = Instant.now();
    }
    
    public void markFailed(String reason) {
        this.status = PayoutStatus.FAILED;
        this.failureReason = reason;
        this.failedAt = Instant.now();
    }
}