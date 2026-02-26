package com.payment.notification.service;

import org.springframework.boot.web.client.RestTemplateBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailNotificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final RestTemplate restTemplate;

    public EmailNotificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            RestTemplateBuilder restTemplateBuilder) {
        this.mailSenderProvider = mailSenderProvider;
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.provider:smtp}")
    private String provider;

    @Value("${notification.email.from:no-reply@payment-system.local}")
    private String fromAddress;

    @Value("${notification.email.sendgrid.api-key:}")
    private String sendGridApiKey;

    @Value("${notification.email.sendgrid.url:https://api.sendgrid.com/v3/mail/send}")
    private String sendGridUrl;

    @Async("webhookExecutor")
    public void sendPaymentEventEmail(Map<String, Object> event) {
        String eventType = asString(event.get("eventType"));
        String recipient = resolveRecipient(event);

        if (!StringUtils.hasText(recipient)) {
            log.debug("Skipping email notification (no recipient in event): type={}", eventType);
            return;
        }

        String subject = buildSubject(eventType);
        String body = buildBody(eventType, event);

        if (!emailEnabled) {
            log.info("Email notifications disabled (MVP fallback). Would send email to={} subject={}", recipient, subject);
            return;
        }

        try {
            if ("sendgrid".equalsIgnoreCase(provider)) {
                sendViaSendGrid(recipient, subject, body, eventType);
            } else {
                sendViaSmtp(recipient, subject, body, eventType);
            }
        } catch (Exception e) {
            log.error("Failed to send payment notification email: to={}, type={}", recipient, eventType, e);
        }
    }

    private void sendViaSmtp(String recipient, String subject, String body, String eventType) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("SMTP email provider selected but JavaMailSender not configured. recipient={}", recipient);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Payment notification email sent via SMTP: to={}, type={}", recipient, eventType);
    }

    private void sendViaSendGrid(String recipient, String subject, String body, String eventType) {
        if (!StringUtils.hasText(sendGridApiKey)) {
            log.warn("SendGrid provider selected but API key missing. recipient={}", recipient);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(sendGridApiKey.trim());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = Map.of(
            "personalizations", List.of(Map.of(
                "to", List.of(Map.of("email", recipient))
            )),
            "from", Map.of("email", fromAddress),
            "subject", subject,
            "content", List.of(Map.of(
                "type", "text/plain",
                "value", body
            ))
        );

        restTemplate.postForEntity(sendGridUrl, new HttpEntity<>(payload, headers), Void.class);
        log.info("Payment notification email sent via SendGrid: to={}, type={}", recipient, eventType);
    }

    private String resolveRecipient(Map<String, Object> event) {
        // Prefer event top-level customerEmail.
        String recipient = asString(event.get("customerEmail"));
        if (StringUtils.hasText(recipient)) {
            return recipient;
        }

        // Try nested customer.email payload if present.
        Object customer = event.get("customer");
        if (customer instanceof Map<?, ?> customerMap) {
            Object email = customerMap.get("email");
            recipient = asString(email);
            if (StringUtils.hasText(recipient)) {
                return recipient;
            }
        }

        // Fallback to metadata.notificationEmail / metadata.email.
        Object metadata = event.get("metadata");
        if (metadata instanceof Map<?, ?> metaMap) {
            recipient = asString(metaMap.get("notificationEmail"));
            if (StringUtils.hasText(recipient)) {
                return recipient;
            }
            recipient = asString(metaMap.get("email"));
        }

        return recipient;
    }

    private String buildSubject(String eventType) {
        return switch (eventType == null ? "" : eventType) {
            case "payment.captured" -> "Payment captured successfully";
            case "payment.failed" -> "Payment failed";
            case "payment.refunded" -> "Payment refunded";
            case "payment.authorized" -> "Payment authorized";
            default -> "Payment event notification";
        };
    }

    private String buildBody(String eventType, Map<String, Object> event) {
        String paymentId = asString(event.get("paymentId"));
        Object amount = event.get("amount");
        String currency = asString(event.get("currency"));
        String merchantId = asString(event.get("merchantId"));
        String status = asString(event.get("status"));

        return """
            Payment Event Notification

            Event Type: %s
            Payment ID: %s
            Merchant ID: %s
            Amount: %s %s
            Status: %s

            Generated by notification-service.
            """.formatted(
            safe(eventType),
            safe(paymentId),
            safe(merchantId),
            amount != null ? amount : "N/A",
            safe(currency),
            safe(status)
        );
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "N/A";
    }
}
