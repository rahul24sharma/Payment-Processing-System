package com.payment.service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Payment method information")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentMethodResponse {
    
    @Schema(description = "Payment method ID", example = "pm_1234567890")
    private String id;
    
    @Schema(description = "Payment method type", example = "card")
    private String type;
    
    @Schema(description = "Card details")
    private CardDetails card;
    
    @Schema(description = "Bank account details")
    private BankAccountDetails bankAccount;
    
    @Schema(description = "Wallet provider")
    private String walletProvider;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CardDetails {
        @Schema(description = "Card brand", example = "visa")
        private String brand;
        
        @Schema(description = "Last 4 digits", example = "4242")
        private String last4;
        
        @Schema(description = "Expiration month (1-12)", example = "12")
        private Integer expMonth;
        
        @Schema(description = "Expiration year", example = "2027")
        private Integer expYear;
        
        @Schema(description = "Whether card is expired", example = "false")
        private Boolean expired;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BankAccountDetails {
        @Schema(description = "Bank name", example = "Chase")
        private String bankName;
        
        @Schema(description = "Last 4 digits", example = "6789")
        private String last4;
    }
}