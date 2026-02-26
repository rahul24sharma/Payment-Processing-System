package com.payment.merchant.service;

import com.payment.merchant.dto.*;
import com.payment.merchant.entity.SupportTicket;
import com.payment.merchant.entity.SupportTicketComment;
import com.payment.merchant.repository.SupportTicketCommentRepository;
import com.payment.merchant.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TicketService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("OPEN", "IN_PROGRESS", "WAITING_ON_MERCHANT", "RESOLVED", "CLOSED");
    private static final Set<String> ALLOWED_PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("GENERAL", "PAYMENTS", "REFUNDS", "WEBHOOKS", "API_KEYS", "ACCOUNT");

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketCommentRepository commentRepository;

    @Transactional(readOnly = true)
    public List<TicketResponse> listTickets(UUID merchantId, String status) {
        List<SupportTicket> tickets = (status == null || status.isBlank())
            ? ticketRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
            : ticketRepository.findByMerchantIdAndStatusOrderByCreatedAtDesc(merchantId, normalizeStatus(status));

        return tickets.stream().map(ticket -> toResponse(ticket, false)).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(UUID merchantId, UUID ticketId) {
        SupportTicket ticket = ticketRepository.findByIdAndMerchantId(ticketId, merchantId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        return toResponse(ticket, true);
    }

    public TicketResponse createTicket(UUID merchantId, CreateTicketRequest request) {
        SupportTicket ticket = SupportTicket.builder()
            .merchantId(merchantId)
            .title(request.getTitle().trim())
            .description(request.getDescription().trim())
            .category(normalizeCategory(request.getCategory()))
            .priority(normalizePriority(request.getPriority()))
            .status("OPEN")
            .build();

        ticket = ticketRepository.save(ticket);

        // Initial comment preserves conversation history.
        SupportTicketComment comment = SupportTicketComment.builder()
            .ticket(ticket)
            .merchantId(merchantId)
            .authorType("MERCHANT")
            .message(request.getDescription().trim())
            .build();
        commentRepository.save(comment);

        log.info("Support ticket created: id={}, merchantId={}", ticket.getId(), merchantId);
        return getTicket(merchantId, ticket.getId());
    }

    public TicketResponse addComment(UUID merchantId, UUID ticketId, AddTicketCommentRequest request) {
        SupportTicket ticket = ticketRepository.findByIdAndMerchantId(ticketId, merchantId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        SupportTicketComment comment = SupportTicketComment.builder()
            .ticket(ticket)
            .merchantId(merchantId)
            .authorType("MERCHANT")
            .message(request.getMessage().trim())
            .build();
        commentRepository.save(comment);

        if ("RESOLVED".equals(ticket.getStatus()) || "CLOSED".equals(ticket.getStatus())) {
            ticket.setStatus("WAITING_ON_MERCHANT");
            ticket.setClosedAt(null);
            ticketRepository.save(ticket);
        }

        log.info("Support ticket comment added: ticketId={}, merchantId={}", ticketId, merchantId);
        return getTicket(merchantId, ticketId);
    }

    public TicketResponse updateStatus(UUID merchantId, UUID ticketId, UpdateTicketStatusRequest request) {
        SupportTicket ticket = ticketRepository.findByIdAndMerchantId(ticketId, merchantId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        String status = normalizeStatus(request.getStatus());
        ticket.setStatus(status);
        ticket.setClosedAt(("RESOLVED".equals(status) || "CLOSED".equals(status)) ? Instant.now() : null);
        ticketRepository.save(ticket);

        log.info("Support ticket status updated: ticketId={}, status={}, merchantId={}", ticketId, status, merchantId);
        return getTicket(merchantId, ticketId);
    }

    private TicketResponse toResponse(SupportTicket ticket, boolean includeComments) {
        List<TicketCommentResponse> comments = includeComments
            ? commentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()).stream()
                .map(this::toCommentResponse)
                .toList()
            : List.of();

        return TicketResponse.builder()
            .id(ticket.getId())
            .merchantId(ticket.getMerchantId())
            .title(ticket.getTitle())
            .description(ticket.getDescription())
            .category(ticket.getCategory())
            .priority(ticket.getPriority())
            .status(ticket.getStatus())
            .createdAt(ticket.getCreatedAt())
            .updatedAt(ticket.getUpdatedAt())
            .closedAt(ticket.getClosedAt())
            .comments(comments)
            .build();
    }

    private TicketCommentResponse toCommentResponse(SupportTicketComment comment) {
        return TicketCommentResponse.builder()
            .id(comment.getId())
            .authorType(comment.getAuthorType())
            .message(comment.getMessage())
            .createdAt(comment.getCreatedAt())
            .build();
    }

    private String normalizeStatus(String value) {
        String normalized = normalizeEnumLike(value, "OPEN");
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid ticket status");
        }
        return normalized;
    }

    private String normalizePriority(String value) {
        String normalized = normalizeEnumLike(value, "MEDIUM");
        if (!ALLOWED_PRIORITIES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid ticket priority");
        }
        return normalized;
    }

    private String normalizeCategory(String value) {
        String normalized = normalizeEnumLike(value, "GENERAL");
        if (!ALLOWED_CATEGORIES.contains(normalized)) {
            return "GENERAL";
        }
        return normalized;
    }

    private String normalizeEnumLike(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }
}
