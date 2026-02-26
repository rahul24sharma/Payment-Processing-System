package com.payment.service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Customer address information")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @Size(max = 255, message = "Address line 1 cannot exceed 255 characters")
    private String line1;

    @Size(max = 255, message = "Address line 2 cannot exceed 255 characters")
    private String line2;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    private String postalCode;

    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be a 2-letter ISO code")
    private String country;
}
