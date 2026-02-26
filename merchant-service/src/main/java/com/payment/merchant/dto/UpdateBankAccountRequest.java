package com.payment.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBankAccountRequest {

    @NotBlank(message = "Account holder name is required")
    @Size(max = 255, message = "Account holder name cannot exceed 255 characters")
    private String accountHolderName;

    @NotBlank(message = "Account number is required")
    @Size(min = 4, max = 34, message = "Account number must be between 4 and 34 characters")
    private String accountNumber;

    @NotBlank(message = "Routing number is required")
    @Pattern(regexp = "\\d{9}", message = "Routing number must be 9 digits")
    private String routingNumber;

    @NotBlank(message = "Account type is required")
    private String accountType;
}
