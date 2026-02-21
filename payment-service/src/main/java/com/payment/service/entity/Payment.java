package com.payment.service.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_merchant_id", columnList = "merchant_id"),
    @Index(name = "idx_payments_customer_id", columnList = "customer_id"),
    @Index(name = "idx_payments_status", columnList = "status"),
    @Index(name = "idx_payments_created_at", columnList = "created_at"),
    @Index(name = "idx_payments_merchant_created", columnList = "merchant_id, created_at"),
    @Index(name = "idx_payments_idempotency", columnList = "idempotency_key", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;
    
    @Column(name = "customer_id")
    private UUID customerId;
    
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money amount;
    
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "payment_status")
    private PaymentStatus status;
    
    @Column(name = "payment_method_id")
    private UUID paymentMethodId;
    
    @Column(name = "processor")
    private String processor;
    
    @Column(name = "processor_payment_id")
    private String processorPaymentId;
    
    @Column(name = "fraud_score", precision = 5, scale = 2)
    private BigDecimal fraudScore;
    
    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    @Column(name = "failure_code")
    private String failureCode;
    
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "authorized_at")
    private Instant authorizedAt;
    
    @Column(name = "captured_at")
    private Instant capturedAt;
    
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Refund> refunds = new ArrayList<>();
    
    /**
     * State machine: Allowed transitions
     */
    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = Map.of(
        PaymentStatus.PENDING, Set.of(
            PaymentStatus.AUTHORIZED, 
            PaymentStatus.FAILED, 
            PaymentStatus.DECLINED
        ),
        PaymentStatus.AUTHORIZED, Set.of(
            PaymentStatus.CAPTURED, 
            PaymentStatus.VOID, 
            PaymentStatus.EXPIRED,
            PaymentStatus.FAILED
        ),
        PaymentStatus.CAPTURED, Set.of(
            PaymentStatus.REFUNDED,
            PaymentStatus.PARTIALLY_REFUNDED
        ),
        PaymentStatus.PARTIALLY_REFUNDED, Set.of(
            PaymentStatus.REFUNDED
        )
    );
    
    /**
     * Check if payment can transition to new status
     */
    public boolean canTransitionTo(PaymentStatus newStatus) {
        Set<PaymentStatus> allowedStatuses = ALLOWED_TRANSITIONS.get(this.status);
        return allowedStatuses != null && allowedStatuses.contains(newStatus);
    }
    
    /**
     * Calculate remaining refundable amount
     */
    public Money getRemainingRefundableAmount() {
        if (status != PaymentStatus.CAPTURED && 
            status != PaymentStatus.PARTIALLY_REFUNDED) {
            return Money.zero(amount.getCurrency());
        }
        
        Money totalRefunded = refunds.stream()
            .filter(r -> r.getStatus() == RefundStatus.SUCCEEDED)
            .map(Refund::getAmount)
            .reduce(Money.zero(amount.getCurrency()), Money::add);
        
        return amount.subtract(totalRefunded);
    }
    
    /**
     * Add a refund to this payment
     */
    public void addRefund(Refund refund) {
        this.refunds.add(refund);
        refund.setPayment(this);
        
        // Update payment status
        Money remaining = getRemainingRefundableAmount();
        if (remaining.isZero()) {
            this.status = PaymentStatus.REFUNDED;
        } else {
            this.status = PaymentStatus.PARTIALLY_REFUNDED;
        }
    }
    
    /**
     * Lifecycle callbacks
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    /**
     * Business methods for state transitions
     */
    public void authorize() {
        if (!canTransitionTo(PaymentStatus.AUTHORIZED)) {
            throw new IllegalStateException(
                "Cannot authorize payment in status: " + this.status);
        }
        this.status = PaymentStatus.AUTHORIZED;
        this.authorizedAt = Instant.now();
    }
    
    public void capture() {
        if (!canTransitionTo(PaymentStatus.CAPTURED)) {
            throw new IllegalStateException(
                "Cannot capture payment in status: " + this.status);
        }
        this.status = PaymentStatus.CAPTURED;
        this.capturedAt = Instant.now();
    }
    
    public void voidPayment() {
        if (!canTransitionTo(PaymentStatus.VOID)) {
            throw new IllegalStateException(
                "Cannot void payment in status: " + this.status);
        }
        this.status = PaymentStatus.VOID;
    }
    
    public void markFailed(String reason, String code) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.failureCode = code;
    }
    
    public void markDeclined(String reason) {
        this.status = PaymentStatus.DECLINED;
        this.failureReason = reason;
    }
}