package com.payment.service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "Payment response object")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
    
    @Schema(description = "Unique payment identifier", example = "pay_7d8f9e2a1b3c")
    private String id;
    
    @Schema(description = "Object type", example = "payment")
    @Builder.Default
    private String object = "payment";
    
    @Schema(description = "Payment amount in cents", example = "10000")
    private Long amount;
    
    @Schema(description = "Three-letter ISO currency code", example = "usd")
    private String currency;
    
    @Schema(
        description = "Payment status",
        example = "authorized",
        allowableValues = {"pending", "authorized", "captured", "refunded", "failed", "declined"}
    )
    private String status;
    
    @Schema(description = "Whether payment has been captured", example = "false")
    private Boolean captured;
    
    @Schema(description = "Payment method used")
    private PaymentMethodResponse paymentMethod;
    
    @Schema(description = "Customer information")
    private CustomerResponse customer;
    
    @Schema(description = "Fraud detection details")
    private FraudDetailsResponse fraudDetails;
    
    @Schema(description = "List of refunds")
    private List<RefundResponse> refunds;
    
    @Schema(description = "Additional metadata")
    private Map<String, Object> metadata;
    
    @Schema(description = "Payment description")
    private String description;
    
    @Schema(description = "Failure reason (if failed)")
    private String failureReason;
    
    @Schema(description = "Failure code (if failed)")
    private String failureCode;
    
    @Schema(description = "Created timestamp (ISO 8601)", example = "2026-02-15T10:30:00Z")
    private String createdAt;
    
    @Schema(description = "Authorized timestamp")
    private String authorizedAt;
    
    @Schema(description = "Captured timestamp")
    private String capturedAt;

    @Schema(description = "Next customer action required to complete the payment (e.g. Stripe 3DS)")
    private PaymentNextActionResponse nextAction;
}
