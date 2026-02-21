package com.payment.service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "Paginated list of payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentListResponse {
    
    @Schema(description = "List of payments")
    private List<PaymentResponse> data;
    
    @Schema(description = "Whether there are more results", example = "true")
    private Boolean hasMore;
    
    @Schema(description = "Cursor for next page", example = "MjAyNi0wMi0xNVQxMDozMDowMFo6cGF5XzEyMw==")
    private String nextCursor;
    
    @Schema(description = "Total count (optional)")
    private Long totalCount;
}