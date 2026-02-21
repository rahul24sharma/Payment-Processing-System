package com.payment.service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_methods", indexes = {
    @Index(name = "idx_payment_methods_customer", columnList = "customer_id"),
    @Index(name = "idx_payment_methods_token", columnList = "token")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethodType type;
    
    /**
     * IMPORTANT: This is a TOKEN, never store raw card numbers!
     * Token comes from client-side tokenization (Stripe Elements, etc.)
     */
    @Column(nullable = false)
    private String token;
    
    // Card details (for display purposes only)
    @Column(name = "card_brand")
    private String cardBrand; // visa, mastercard, amex
    
    @Column(name = "card_last4", length = 4)
    private String cardLast4;
    
    @Column(name = "card_exp_month")
    private Integer cardExpMonth; // 1-12
    
    @Column(name = "card_exp_year")
    private Integer cardExpYear; // 2026, 2027, etc.
    
    // Bank account details (tokenized)
    @Column(name = "bank_name")
    private String bankName;
    
    @Column(name = "bank_account_last4", length = 4)
    private String bankAccountLast4;
    
    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
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
    
    /**
     * Check if card is expired
     */
    public boolean isExpired() {
        if (type != PaymentMethodType.CARD || cardExpYear == null || cardExpMonth == null) {
            return false;
        }
        
        Instant now = Instant.now();
        int currentYear = now.atZone(java.time.ZoneOffset.UTC).getYear();
        int currentMonth = now.atZone(java.time.ZoneOffset.UTC).getMonthValue();
        
        if (cardExpYear < currentYear) {
            return true;
        }
        
        if (cardExpYear == currentYear && cardExpMonth < currentMonth) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get masked display string
     */
    public String getDisplayString() {
        return switch (type) {
            case CARD -> String.format("%s •••• %s", 
                cardBrand != null ? cardBrand.toUpperCase() : "CARD", 
                cardLast4);
            case BANK_ACCOUNT -> String.format("Bank •••• %s", bankAccountLast4);
            case WALLET -> "Digital Wallet";
        };
    }
}