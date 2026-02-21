package com.payment.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Customer information")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {
    
    @Schema(
        description = "Customer ID (if existing customer)",
        example = "cus_1234567890"
    )
    private String id;
    
    @Schema(
        description = "Customer email",
        example = "customer@example.com"
    )
    @Email(message = "Invalid email format")
    private String email;
    
    @Schema(
        description = "Customer name",
        example = "John Doe"
    )
    @Size(max = 255, message = "Name cannot exceed 255 characters")
    private String name;
    
    @Schema(
        description = "Customer phone",
        example = "+1234567890"
    )
    @Size(max = 50, message = "Phone cannot exceed 50 characters")
    private String phone;
}