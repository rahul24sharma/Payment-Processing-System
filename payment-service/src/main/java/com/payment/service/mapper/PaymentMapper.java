package com.payment.service.mapper;

import com.payment.service.dto.request.CreatePaymentRequest;
import com.payment.service.dto.request.CustomerRequest;
import com.payment.service.dto.response.*;
import com.payment.service.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PaymentMapper {
    
    /**
     * Convert Payment entity to PaymentResponse DTO
     */
    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }
        
        return PaymentResponse.builder()
            .id(payment.getId().toString())
            .object("payment")
            .amount(payment.getAmount().getAmountInCents())
            .currency(payment.getAmount().getCurrency().toLowerCase())
            .status(payment.getStatus().name().toLowerCase())
            .captured(payment.getStatus() == PaymentStatus.CAPTURED || 
                     payment.getStatus() == PaymentStatus.REFUNDED ||
                     payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED)
            .paymentMethod(toPaymentMethodResponse(payment.getPaymentMethodId()))
            .customer(toCustomerResponse(payment.getCustomerId()))
            .fraudDetails(toFraudDetailsResponse(payment))
            .refunds(toRefundResponseList(payment.getRefunds()))
            .metadata(payment.getMetadata())
            .failureReason(payment.getFailureReason())
            .failureCode(payment.getFailureCode())
            .createdAt(payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null)
            .authorizedAt(payment.getAuthorizedAt() != null ? payment.getAuthorizedAt().toString() : null)
            .capturedAt(payment.getCapturedAt() != null ? payment.getCapturedAt().toString() : null)
            .build();
    }
    
    /**
     * Convert Payment entity to minimal response (for lists)
     */
    public PaymentResponse toMinimalResponse(Payment payment) {
        if (payment == null) {
            return null;
        }
        
        return PaymentResponse.builder()
            .id(payment.getId().toString())
            .object("payment")
            .amount(payment.getAmount().getAmountInCents())
            .currency(payment.getAmount().getCurrency().toLowerCase())
            .status(payment.getStatus().name().toLowerCase())
            .captured(payment.getStatus() == PaymentStatus.CAPTURED)
            .createdAt(payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null)
            .build();
    }
    
    /**
     * Convert Refund entity to RefundResponse DTO
     */
    public RefundResponse toRefundResponse(Refund refund) {
        if (refund == null) {
            return null;
        }
        
        return RefundResponse.builder()
            .id(refund.getId().toString())
            .object("refund")
            .paymentId(refund.getPayment() != null ? refund.getPayment().getId().toString() : null)
            .amount(refund.getAmount().getAmountInCents())
            .currency(refund.getAmount().getCurrency().toLowerCase())
            .status(refund.getStatus().name().toLowerCase())
            .reason(refund.getReason())
            .failureReason(refund.getFailureReason())
            .createdAt(refund.getCreatedAt() != null ? refund.getCreatedAt().toString() : null)
            .completedAt(refund.getCompletedAt() != null ? refund.getCompletedAt().toString() : null)
            .build();
    }
    
    /**
     * Convert list of refunds
     */
    private List<RefundResponse> toRefundResponseList(List<Refund> refunds) {
        if (refunds == null || refunds.isEmpty()) {
            return null;
        }
        
        return refunds.stream()
            .map(this::toRefundResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Create payment method response (simplified for MVP)
     */
    private PaymentMethodResponse toPaymentMethodResponse(java.util.UUID paymentMethodId) {
        if (paymentMethodId == null) {
            return null;
        }
        
        // For MVP, we'll return minimal info
        // In full version, we'd fetch from PaymentMethodRepository
        return PaymentMethodResponse.builder()
            .id(paymentMethodId.toString())
            .type("card")
            .card(PaymentMethodResponse.CardDetails.builder()
                .brand("visa")
                .last4("4242")
                .expMonth(12)
                .expYear(2027)
                .expired(false)
                .build())
            .build();
    }
    
    /**
     * Create customer response (simplified for MVP)
     */
    private CustomerResponse toCustomerResponse(java.util.UUID customerId) {
        if (customerId == null) {
            return null;
        }
        
        // For MVP, we'll return minimal info
        // In full version, we'd fetch from CustomerRepository
        return CustomerResponse.builder()
            .id(customerId.toString())
            .build();
    }
    
    /**
     * Create fraud details response
     */
    private FraudDetailsResponse toFraudDetailsResponse(Payment payment) {
        if (payment.getFraudScore() == null) {
            return null;
        }
        
        String riskLevel = determineRiskLevel(payment.getFraudScore());
        
        return FraudDetailsResponse.builder()
            .score(payment.getFraudScore())
            .riskLevel(riskLevel)
            .build();
    }
    
    /**
     * Determine risk level from score
     */
    private String determineRiskLevel(java.math.BigDecimal score) {
        if (score.compareTo(java.math.BigDecimal.valueOf(75)) >= 0) {
            return "high";
        } else if (score.compareTo(java.math.BigDecimal.valueOf(50)) >= 0) {
            return "medium";
        } else if (score.compareTo(java.math.BigDecimal.valueOf(25)) >= 0) {
            return "low";
        } else {
            return "very_low";
        }
    }
    
    /**
     * Convert CreatePaymentRequest to Payment entity
     */
    public Payment toEntity(CreatePaymentRequest request) {
        return Payment.builder()
            .amount(Money.of(request.getAmount(), request.getCurrency()))
            .status(PaymentStatus.PENDING)
            .metadata(request.getMetadata())
            .build();
    }
}