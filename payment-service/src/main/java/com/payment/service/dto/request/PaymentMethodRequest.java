package com.payment.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Payment method details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodRequest {
    
    @Schema(
        description = "Payment method type",
        example = "card",
        allowableValues = {"card", "bank_account", "wallet"}
    )
    @NotNull(message = "Payment method type is required")
    @Pattern(regexp = "^(card|bank_account|wallet)$", 
             message = "Type must be: card, bank_account, or wallet")
    private String type;
    
    @Schema(
        description = "Tokenized card (from client-side tokenization)",
        example = "tok_visa_4242424242424242"
    )
    private String cardToken;
    
    @Schema(
        description = "Tokenized bank account",
        example = "btok_1234567890"
    )
    private String bankToken;
    
    @Schema(
        description = "Wallet provider (apple_pay, google_pay)",
        example = "apple_pay"
    )
    private String walletProvider;
    
    @Schema(
        description = "Saved payment method ID (use existing)",
        example = "pm_1234567890"
    )
    private String savedPaymentMethodId;
}