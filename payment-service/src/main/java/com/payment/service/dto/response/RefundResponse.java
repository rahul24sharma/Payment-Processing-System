package com.payment.service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Refund response object")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RefundResponse {
    
    @Schema(description = "Unique refund identifier", example = "ref_abc123xyz")
    private String id;
    
    @Schema(description = "Object type", example = "refund")
    @Builder.Default
    private String object = "refund";
    
    @Schema(description = "Payment ID this refund belongs to", example = "pay_7d8f9e2a1b3c")
    private String paymentId;
    
    @Schema(description = "Refund amount in cents", example = "5000")
    private Long amount;
    
    @Schema(description = "Currency", example = "usd")
    private String currency;
    
    @Schema(
        description = "Refund status",
        example = "succeeded",
        allowableValues = {"pending", "processing", "succeeded", "failed", "cancelled"}
    )
    private String status;
    
    @Schema(description = "Refund reason", example = "customer_request")
    private String reason;
    
    @Schema(description = "Failure reason (if failed)")
    private String failureReason;
    
    @Schema(description = "Created timestamp", example = "2026-02-15T10:30:00Z")
    private String createdAt;
    
    @Schema(description = "Completed timestamp")
    private String completedAt;
}