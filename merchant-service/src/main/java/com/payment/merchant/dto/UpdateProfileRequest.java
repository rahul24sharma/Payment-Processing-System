package com.payment.merchant.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    
    @Size(min = 2, max = 255, message = "Business name must be between 2 and 255 characters")
    private String businessName;
    
    @Size(max = 50, message = "Phone cannot exceed 50 characters")
    private String phone;
    
    @Size(max = 255, message = "Website cannot exceed 255 characters")
    private String website;
}