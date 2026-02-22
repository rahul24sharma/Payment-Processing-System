package com.payment.settlement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Junction table linking payouts to payments
 */
@Entity
@Table(name = "payout_payments", indexes = {
    @Index(name = "idx_payout_payments_payout", columnList = "payout_id"),
    @Index(name = "idx_payout_payments_payment", columnList = "payment_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutPayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_id", nullable = false)
    private Payout payout;
    
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
}