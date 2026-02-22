package com.payment.ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries", indexes = {
    @Index(name = "idx_ledger_entry_group", columnList = "entry_group_id"),
    @Index(name = "idx_ledger_account", columnList = "account_id"),
    @Index(name = "idx_ledger_account_date", columnList = "account_id, created_at"),
    @Index(name = "idx_ledger_payment", columnList = "payment_id"),
    @Index(name = "idx_ledger_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "entry_group_id", nullable = false)
    private UUID entryGroupId;
    
    @Column(name = "account_id", nullable = false)
    private UUID accountId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;
    
    @Column(name = "debit_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal debitAmount;
    
    @Column(name = "credit_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal creditAmount;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Column(name = "payment_id")
    private UUID paymentId;
    
    @Column(name = "refund_id")
    private UUID refundId;
    
    @Column(name = "settlement_id")
    private UUID settlementId;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        
        // Validate: either debit OR credit, not both
        validateAmounts();
    }
    
    private void validateAmounts() {
        boolean debitIsZero = debitAmount.compareTo(BigDecimal.ZERO) == 0;
        boolean creditIsZero = creditAmount.compareTo(BigDecimal.ZERO) == 0;
        
        if (debitIsZero && creditIsZero) {
            throw new IllegalStateException("Both debit and credit cannot be zero");
        }
        
        if (!debitIsZero && !creditIsZero) {
            throw new IllegalStateException("Both debit and credit cannot be non-zero");
        }
        
        if (debitAmount.compareTo(BigDecimal.ZERO) < 0 || 
            creditAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Amounts must be positive");
        }
    }
}