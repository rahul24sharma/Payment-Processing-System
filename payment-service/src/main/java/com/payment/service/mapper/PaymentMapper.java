package com.payment.service.mapper;

import com.payment.service.dto.request.CreatePaymentRequest;
import com.payment.service.dto.response.*;
import com.payment.service.entity.*;
import com.payment.service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaymentMapper {
    private final CustomerRepository customerRepository;
    
    /**
     * Convert Payment entity to PaymentResponse DTO
     */
    public PaymentResponse toResponse(Payment payment) {
        return toResponse(payment, null);
    }

    public PaymentResponse toResponse(Payment payment, PaymentNextActionResponse nextAction) {
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
            .paymentMethod(toPaymentMethodResponse(payment))
            .customer(toCustomerResponse(payment.getCustomerId()))
            .fraudDetails(toFraudDetailsResponse(payment))
            .refunds(toRefundResponseList(payment.getRefunds()))
            .metadata(payment.getMetadata())
            .failureReason(payment.getFailureReason())
            .failureCode(payment.getFailureCode())
            .createdAt(payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null)
            .authorizedAt(payment.getAuthorizedAt() != null ? payment.getAuthorizedAt().toString() : null)
            .capturedAt(payment.getCapturedAt() != null ? payment.getCapturedAt().toString() : null)
            .nextAction(nextAction)
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
    private PaymentMethodResponse toPaymentMethodResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        String stripePaymentMethodId = null;
        if (payment.getMetadata() != null) {
            Object metadataValue = payment.getMetadata().get("stripe_payment_method_id");
            if (metadataValue instanceof String value && !value.isBlank()) {
                stripePaymentMethodId = value;
            }
        }

        if (stripePaymentMethodId == null && payment.getPaymentMethodId() == null) {
            return null;
        }

        return PaymentMethodResponse.builder()
            .id(stripePaymentMethodId != null ? stripePaymentMethodId : payment.getPaymentMethodId().toString())
            .type(payment.getProcessor() != null && payment.getProcessor().equalsIgnoreCase("stripe") ? "card" : "unknown")
            .build();
    }
    
    /**
     * Create customer response (simplified for MVP)
     */
    private CustomerResponse toCustomerResponse(java.util.UUID customerId) {
        if (customerId == null) {
            return null;
        }

        return customerRepository.findById(customerId)
            .map(customer -> CustomerResponse.builder()
                .id(customer.getId().toString())
                .email(customer.getEmail())
                .name(customer.getName())
                .phone(customer.getPhone())
                .build())
            .orElse(CustomerResponse.builder()
                .id(customerId.toString())
                .build());
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
