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
@Table(name = "account_balances", indexes = {
    @Index(name = "idx_account_balances_type", columnList = "account_type"),
    @Index(name = "idx_account_balances_updated", columnList = "last_updated")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalance {
    
    @Id
    private UUID accountId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }
    
    public void addToBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
    
    public void subtractFromBalance(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }
}