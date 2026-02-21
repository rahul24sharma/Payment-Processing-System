package com.payment.service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Fraud detection details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FraudDetailsResponse {
    
    @Schema(description = "Fraud score (0-100)", example = "15.5")
    private BigDecimal score;
    
    @Schema(
        description = "Risk level",
        example = "low",
        allowableValues = {"very_low", "low", "medium", "high", "critical"}
    )
    private String riskLevel;
    
    @Schema(description = "Factors contributing to fraud score")
    private Map<String, Object> factors;
}