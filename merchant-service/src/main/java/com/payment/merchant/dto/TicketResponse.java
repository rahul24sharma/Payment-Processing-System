package com.payment.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private UUID id;
    private UUID merchantId;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;
    private List<TicketCommentResponse> comments;
}
