package com.payment.service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.service.entity.StripeWebhookEvent;
import com.payment.service.repository.StripeWebhookEventRepository;
import com.payment.service.service.PaymentService;
import com.payment.service.service.StripePaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final StripePaymentService stripePaymentService;
    private final PaymentService paymentService;
    private final StripeWebhookEventRepository stripeWebhookEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${spring.stripe.webhook-secret:${stripe.webhook-secret:}}")
    private String stripeWebhookSecret;

    @PostMapping("/stripe")
    public ResponseEntity<Map<String, Object>> handleStripeWebhook(
        @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature,
        @RequestBody String payload
    ) {
        if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()) {
            log.warn("Stripe webhook received but webhook secret is not configured");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("received", false, "error", "stripe_webhook_not_configured"));
        }

        if (stripeSignature == null || stripeSignature.isBlank()
            || !stripePaymentService.verifyWebhookSignature(payload, stripeSignature, stripeWebhookSecret)) {
            log.warn("Stripe webhook signature verification failed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("received", false, "error", "invalid_signature"));
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventId = text(root, "id");
            String eventType = text(root, "type");
            JsonNode objectNode = root.path("data").path("object");

            if (eventId == null || eventId.isBlank()) {
                log.warn("Stripe webhook missing event id");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("received", false, "error", "missing_event_id"));
            }

            if (!reserveWebhookEvent(eventId, eventType)) {
                log.info("Duplicate Stripe webhook ignored: eventId={}, type={}", eventId, eventType);
                return ResponseEntity.ok(Map.of("received", true, "duplicate", true, "id", eventId, "type", eventType));
            }

            log.info("Received Stripe webhook: id={}, type={}", eventId, eventType);

            try {
                switch (eventType) {
                    case "payment_intent.succeeded" -> paymentService.handleStripePaymentIntentSucceededWebhook(text(objectNode, "id"));
                    case "payment_intent.payment_failed" -> {
                        JsonNode lastError = objectNode.path("last_payment_error");
                        paymentService.handleStripePaymentIntentFailedWebhook(
                            text(objectNode, "id"),
                            text(lastError, "message"),
                            text(lastError, "code")
                        );
                    }
                    case "payment_intent.requires_action" ->
                        paymentService.handleStripePaymentIntentRequiresActionWebhook(text(objectNode, "id"));
                    case "payment_intent.amount_capturable_updated",
                         "payment_intent.requires_capture" ->
                        paymentService.handleStripePaymentIntentRequiresCaptureWebhook(text(objectNode, "id"));
                    case "payment_intent.canceled" ->
                        paymentService.handleStripePaymentIntentCanceledWebhook(text(objectNode, "id"));
                    case "charge.refunded" ->
                        paymentService.handleStripeChargeRefundedWebhook(
                            text(objectNode, "payment_intent"),
                            longValue(objectNode, "amount_refunded"),
                            upper(text(objectNode, "currency"))
                        );
                    case "refund.updated", "charge.refund.updated", "charge.refund.created" ->
                        paymentService.handleStripeRefundWebhook(
                            text(objectNode, "payment_intent"),
                            text(objectNode, "status"),
                            longValue(objectNode, "amount"),
                            upper(text(objectNode, "currency"))
                        );
                    default -> log.info("Ignoring unsupported Stripe webhook type={}", eventType);
                }
            } catch (Exception processingException) {
                stripeWebhookEventRepository.deleteById(eventId);
                throw processingException;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("received", true);
            response.put("id", eventId);
            response.put("type", eventType);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to process Stripe webhook", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("received", false, "error", "webhook_processing_failed"));
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private Long longValue(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asLong();
    }

    private String upper(String value) {
        return value == null ? null : value.toUpperCase();
    }

    private boolean reserveWebhookEvent(String eventId, String eventType) {
        try {
            stripeWebhookEventRepository.save(StripeWebhookEvent.builder()
                .eventId(eventId)
                .eventType(eventType != null ? eventType : "unknown")
                .build());
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            return false;
        }
    }
}
