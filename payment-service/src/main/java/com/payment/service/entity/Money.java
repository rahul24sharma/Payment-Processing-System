package com.payment.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Value object representing money with currency
 * Immutable and embedded in entities
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Money {
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    public static Money of(BigDecimal amount, String currency) {
        validateCurrency(currency);
        return new Money(amount, currency);
    }
    
    public static Money of(long amountInCents, String currency) {
        BigDecimal amount = BigDecimal.valueOf(amountInCents)
            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        return new Money(amount, currency);
    }
    
    public static Money zero(String currency) {
        validateCurrency(currency);
        return new Money(BigDecimal.ZERO, currency);
    }
    
    /**
     * Add two money values (must be same currency)
     */
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    /**
     * Subtract two money values (must be same currency)
     */
    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }
    
    /**
     * Multiply by a factor
     */
    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor), this.currency);
    }
    
    /**
     * Check if this amount is greater than another
     */
    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }
    
    /**
     * Check if this amount is less than or equal to another
     */
    public boolean isLessThanOrEqualTo(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) <= 0;
    }
    
    /**
     * Check if amount is zero
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Check if amount is positive
     */
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Get amount in cents (for APIs)
     */
    public long getAmountInCents() {
        return this.amount.multiply(BigDecimal.valueOf(100)).longValue();
    }
    
    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: %s vs %s", this.currency, other.currency)
            );
        }
    }
    
    private static void validateCurrency(String currency) {
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency code: " + currency);
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s %s", currency, amount);
    }
}