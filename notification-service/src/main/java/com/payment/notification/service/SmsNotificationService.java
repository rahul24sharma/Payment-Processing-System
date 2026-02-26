package com.payment.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
public class SmsNotificationService {

    private final RestTemplate restTemplate;

    public SmsNotificationService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notification.sms.provider:mock}")
    private String provider;

    @Value("${notification.sms.from:+10000000000}")
    private String fromNumber;

    @Value("${notification.sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${notification.sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${notification.sms.twilio.messaging-service-sid:}")
    private String twilioMessagingServiceSid;

    @Async("webhookExecutor")
    public void sendPaymentEventSms(Map<String, Object> event) {
        String recipient = resolveRecipient(event);
        String eventType = asString(event.get("eventType"));

        if (!StringUtils.hasText(recipient)) {
            log.debug("Skipping SMS notification (no recipient phone in event): type={}", eventType);
            return;
        }

        String message = buildMessage(eventType, event);

        if (!smsEnabled) {
            log.info("SMS notifications disabled (MVP fallback). Would send SMS to={} message={}",
                recipient, message);
            return;
        }

        if (!"twilio".equalsIgnoreCase(provider) && !"mock".equalsIgnoreCase(provider)) {
            log.warn("Unsupported SMS provider '{}' configured. recipient={}", provider, recipient);
            return;
        }

        try {
            if ("twilio".equalsIgnoreCase(provider)) {
                sendViaTwilio(recipient, message, eventType);
            } else {
                log.info("SMS notification sent via {} (mock): from={} to={} eventType={}",
                    provider, fromNumber, recipient, eventType);
            }
        } catch (Exception e) {
            log.error("Failed to send SMS notification: to={}, eventType={}", recipient, eventType, e);
        }
    }

    private void sendViaTwilio(String recipient, String message, String eventType) {
        if (!StringUtils.hasText(twilioAccountSid) || !StringUtils.hasText(twilioAuthToken)) {
            log.warn("Twilio provider selected but SID/Auth token missing. recipient={}", recipient);
            return;
        }

        String url = "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid.trim() + "/Messages.json";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + basicAuth(twilioAccountSid.trim(), twilioAuthToken.trim()));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", recipient);
        form.add("Body", message);

        if (StringUtils.hasText(twilioMessagingServiceSid)) {
            form.add("MessagingServiceSid", twilioMessagingServiceSid.trim());
        } else {
            form.add("From", fromNumber);
        }

        restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
        log.info("SMS notification sent via Twilio: to={} eventType={}", recipient, eventType);
    }

    private String basicAuth(String username, String password) {
        String raw = username + ":" + password;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String resolveRecipient(Map<String, Object> event) {
        String phone = asString(event.get("customerPhone"));
        if (StringUtils.hasText(phone)) {
            return phone;
        }

        Object customer = event.get("customer");
        if (customer instanceof Map<?, ?> customerMap) {
            phone = asString(customerMap.get("phone"));
            if (StringUtils.hasText(phone)) {
                return phone;
            }
        }

        Object metadata = event.get("metadata");
        if (metadata instanceof Map<?, ?> metaMap) {
            phone = asString(metaMap.get("notificationPhone"));
            if (StringUtils.hasText(phone)) {
                return phone;
            }
            phone = asString(metaMap.get("phone"));
        }

        return phone;
    }

    private String buildMessage(String eventType, Map<String, Object> event) {
        String paymentId = asString(event.get("paymentId"));
        String currency = asString(event.get("currency"));
        Object amount = event.get("amount");
        String status = asString(event.get("status"));

        String base = switch (eventType == null ? "" : eventType) {
            case "payment.captured" -> "Payment captured";
            case "payment.failed" -> "Payment failed";
            case "payment.refunded" -> "Payment refunded";
            case "payment.authorized" -> "Payment authorized";
            default -> "Payment update";
        };

        return "%s: %s %s for payment %s (%s)".formatted(
            base,
            amount != null ? amount : "N/A",
            StringUtils.hasText(currency) ? currency : "",
            StringUtils.hasText(paymentId) ? paymentId : "N/A",
            StringUtils.hasText(status) ? status : "unknown"
        ).trim();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
