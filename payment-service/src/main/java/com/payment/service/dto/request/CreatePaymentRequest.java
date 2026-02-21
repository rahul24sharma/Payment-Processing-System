package com.payment.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Schema(description = "Request to create a new payment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {
    
    @Schema(
        description = "Payment amount in smallest currency unit (cents for USD)",
        example = "10000",
        minimum = "50"
    )
    @NotNull(message = "Amount is required")
    @Min(value = 50, message = "Amount must be at least 50 cents")
    private Long amount;
    
    @Schema(
        description = "Three-letter ISO currency code",
        example = "USD"
    )
    @NotNull(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be uppercase ISO code")
    private String currency;
    
    @Schema(description = "Payment method details")
    @NotNull(message = "Payment method is required")
    @Valid
    private PaymentMethodRequest paymentMethod;
    
    @Schema(description = "Customer information")
    @Valid
    private CustomerRequest customer;
    
    @Schema(
        description = "Whether to immediately capture the payment",
        example = "true",
        defaultValue = "true"
    )
    @Builder.Default
    private Boolean capture = true;
    
    @Schema(
        description = "Additional metadata (key-value pairs)",
        example = "{\"order_id\": \"ord_123\", \"product_name\": \"Premium Plan\"}"
    )
    private Map<String, Object> metadata;
    
    @Schema(
        description = "Payment description",
        example = "Monthly subscription payment"
    )
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}