package com.payment.service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Customer information")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerResponse {
    
    @Schema(description = "Customer ID", example = "cus_1234567890")
    private String id;
    
    @Schema(description = "Customer email", example = "customer@example.com")
    private String email;
    
    @Schema(description = "Customer name", example = "John Doe")
    private String name;
    
    @Schema(description = "Customer phone", example = "+1234567890")
    private String phone;
}