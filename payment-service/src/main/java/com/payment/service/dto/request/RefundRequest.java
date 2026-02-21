package com.payment.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Request to create a refund")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {
    
    @Schema(
        description = "Refund amount in cents (optional, defaults to full refund)",
        example = "5000"
    )
    @Min(value = 1, message = "Refund amount must be at least 1 cent")
    private Long amount;
    
    @Schema(
        description = "Reason for refund",
        example = "customer_request",
        allowableValues = {"customer_request", "duplicate", "fraudulent", "other"}
    )
    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    private String reason;
}