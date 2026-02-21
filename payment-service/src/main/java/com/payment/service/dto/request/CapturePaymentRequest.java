package com.payment.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Request to capture an authorized payment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapturePaymentRequest {
    
    @Schema(
        description = "Amount to capture in cents (optional, defaults to full authorized amount)",
        example = "8000"
    )
    @Min(value = 1, message = "Capture amount must be at least 1 cent")
    private Long amount;
}