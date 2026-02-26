package com.payment.merchant.controller;

import com.payment.merchant.dto.*;
import com.payment.merchant.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Tickets", description = "Merchant support tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    @Operation(summary = "List merchant support tickets")
    public ResponseEntity<List<TicketResponse>> listTickets(
        @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader,
        @RequestParam(value = "status", required = false) String status
    ) {
        UUID merchantId = resolveMerchantId(merchantIdHeader);
        if (merchantId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(ticketService.listTickets(merchantId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket details")
    public ResponseEntity<TicketResponse> getTicket(
        @PathVariable UUID id,
        @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader
    ) {
        UUID merchantId = resolveMerchantId(merchantIdHeader);
        if (merchantId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(ticketService.getTicket(merchantId, id));
    }

    @PostMapping
    @Operation(summary = "Create support ticket")
    public ResponseEntity<TicketResponse> createTicket(
        @Valid @RequestBody CreateTicketRequest request,
        @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader
    ) {
        UUID merchantId = resolveMerchantId(merchantIdHeader);
        if (merchantId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(merchantId, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update ticket status")
    public ResponseEntity<TicketResponse> updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateTicketStatusRequest request,
        @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader
    ) {
        UUID merchantId = resolveMerchantId(merchantIdHeader);
        if (merchantId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(ticketService.updateStatus(merchantId, id, request));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add ticket comment")
    public ResponseEntity<TicketResponse> addComment(
        @PathVariable UUID id,
        @Valid @RequestBody AddTicketCommentRequest request,
        @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader
    ) {
        UUID merchantId = resolveMerchantId(merchantIdHeader);
        if (merchantId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(ticketService.addComment(merchantId, id, request));
    }

    private UUID resolveMerchantId(String merchantIdHeader) {
        if (merchantIdHeader == null || merchantIdHeader.isBlank()) {
            return null;
        }
        return UUID.fromString(merchantIdHeader);
    }
}
