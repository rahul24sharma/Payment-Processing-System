package com.payment.settlement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bank transfer service with optional third-party provider integration.
 * Supported modes:
 * - mock (default): local simulation
 * - stripe: Stripe Connect transfer API
 * - http: POST to configured third-party payout endpoint
 *
 * Example providers this can front:
 * - Stripe Connect
 * - Dwolla
 * - ACH/SEPA networks
 * - Wire transfer APIs
 */
@Service
@Slf4j
public class BankTransferService {

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${settlement.bank-transfer.provider:mock}")
    private String provider;

    @Value("${settlement.bank-transfer.http.base-url:}")
    private String httpBaseUrl;

    @Value("${settlement.bank-transfer.http.transfer-path:/v1/transfers}")
    private String httpTransferPath;

    @Value("${settlement.bank-transfer.http.api-key:}")
    private String apiKey;

    @Value("${settlement.bank-transfer.http.auth-header:Authorization}")
    private String authHeaderName;

    @Value("${settlement.bank-transfer.http.auth-prefix:Bearer }")
    private String authHeaderPrefix;

    @Value("${settlement.bank-transfer.http.timeout-ms:5000}")
    private int timeoutMs;

    @Value("${settlement.bank-transfer.stripe.base-url:https://api.stripe.com}")
    private String stripeBaseUrl;

    @Value("${settlement.bank-transfer.stripe.transfer-path:/v1/transfers}")
    private String stripeTransferPath;

    @Value("${settlement.bank-transfer.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${settlement.bank-transfer.stripe.destination-account-default:}")
    private String stripeDefaultDestinationAccount;

    @Value("${settlement.bank-transfer.mock.failure-rate-percent:2}")
    private int mockFailureRatePercent;

    public BankTransferService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplateBuilder = restTemplateBuilder;
    }
    
    /**
     * Transfer funds to merchant's bank account
     */
    public String transfer(UUID merchantId, BigDecimal amount, String currency, String reference) {
        log.info("Initiating bank transfer: merchantId={}, amount={} {}, reference={}", 
            merchantId, amount, currency, reference);

        if (isStripeProviderConfigured()) {
            return transferViaStripeConnect(merchantId, amount, currency, reference);
        }

        if (isHttpProviderConfigured()) {
            return transferViaThirdPartyApi(merchantId, amount, currency, reference);
        }

        return transferViaMock(merchantId, amount, currency, reference);
    }

    private boolean isStripeProviderConfigured() {
        return "stripe".equalsIgnoreCase(provider) || "stripe-connect".equalsIgnoreCase(provider);
    }

    private boolean isHttpProviderConfigured() {
        return "http".equalsIgnoreCase(provider) || "third-party".equalsIgnoreCase(provider);
    }

    private String transferViaStripeConnect(UUID merchantId, BigDecimal amount, String currency, String reference) {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new IllegalStateException(
                "Stripe provider is enabled but settlement.bank-transfer.stripe.secret-key is not configured");
        }

        String destinationAccount = resolveStripeDestinationAccount(merchantId);
        if (destinationAccount == null || destinationAccount.isBlank()) {
            throw new IllegalStateException(
                "Stripe destination account is not configured for merchant " + merchantId +
                ". Configure settlement.bank-transfer.stripe.destination-account-default or add merchant mapping support.");
        }

        String url = buildStripeUrl();
        RestTemplate externalRestTemplate = buildExternalRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(stripeSecretKey);
        headers.set("Idempotency-Key", buildStripeIdempotencyKey(merchantId, reference));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("amount", toStripeMinorUnits(amount, currency).toString());
        form.add("currency", currency.toLowerCase(Locale.ROOT));
        form.add("destination", destinationAccount);
        form.add("description", "Merchant settlement payout");
        form.add("transfer_group", reference);
        form.add("metadata[merchant_id]", merchantId.toString());
        form.add("metadata[reference]", reference);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<Map> response = externalRestTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Stripe transfer failed with status " + response.getStatusCode());
            }

            Map<?, ?> body = response.getBody();
            String transferId = extractTransferId(body);

            log.info("Stripe Connect transfer successful: transferId={}, merchantId={}, destination={}, amount={} {}",
                transferId, merchantId, destinationAccount, amount, currency);

            return transferId;
        } catch (RestClientException e) {
            log.error("Stripe Connect transfer failed: merchantId={}, destination={}", merchantId, destinationAccount, e);
            throw new RuntimeException("Stripe transfer failed: " + e.getMessage(), e);
        }
    }

    private String resolveStripeDestinationAccount(UUID merchantId) {
        // TODO: replace with Merchant Service lookup once merchant payout profile stores Stripe account IDs.
        if (stripeDefaultDestinationAccount != null && !stripeDefaultDestinationAccount.isBlank()) {
            return stripeDefaultDestinationAccount;
        }
        return null;
    }

    private String buildStripeUrl() {
        String base = stripeBaseUrl.endsWith("/") ? stripeBaseUrl.substring(0, stripeBaseUrl.length() - 1) : stripeBaseUrl;
        String path = stripeTransferPath.startsWith("/") ? stripeTransferPath : "/" + stripeTransferPath;
        return base + path;
    }

    private String buildStripeIdempotencyKey(UUID merchantId, String reference) {
        String seed = merchantId + ":" + (reference == null ? "" : reference);
        return "settlement-" + Integer.toHexString(seed.hashCode()) + "-" + System.currentTimeMillis();
    }

    private Long toStripeMinorUnits(BigDecimal amount, String currency) {
        String normalizedCurrency = currency == null ? "" : currency.toUpperCase(Locale.ROOT);
        int scale = isZeroDecimalCurrency(normalizedCurrency) ? 0 : 2;
        return amount.movePointRight(scale).longValueExact();
    }

    private boolean isZeroDecimalCurrency(String currency) {
        return Set.of(
            "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA", "PYG",
            "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF"
        ).contains(currency);
    }

    private String transferViaThirdPartyApi(UUID merchantId, BigDecimal amount, String currency, String reference) {
        if (httpBaseUrl == null || httpBaseUrl.isBlank()) {
            throw new IllegalStateException(
                "Third-party bank transfer provider is enabled but settlement.bank-transfer.http.base-url is not configured");
        }

        String url = buildUrl();
        RestTemplate externalRestTemplate = buildExternalRestTemplate();

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("merchantId", merchantId.toString());
        requestBody.put("amount", amount);
        requestBody.put("currency", currency);
        requestBody.put("reference", reference);
        requestBody.put("channel", "BANK_TRANSFER");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set(authHeaderName, authHeaderPrefix + apiKey);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = externalRestTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Bank transfer provider returned non-success status: " + response.getStatusCode());
            }

            Map<?, ?> body = response.getBody();
            String transferId = extractTransferId(body);

            log.info("Third-party bank transfer successful: provider={}, transferId={}, amount={} {}, status={}",
                provider, transferId, amount, currency, response.getStatusCode().value());

            return transferId;
        } catch (RestClientException e) {
            log.error("Third-party bank transfer failed: provider={}, url={}, merchantId={}",
                provider, url, merchantId, e);
            throw new RuntimeException("Bank transfer failed via provider: " + e.getMessage(), e);
        }
    }

    private RestTemplate buildExternalRestTemplate() {
        int safeTimeoutMs = timeoutMs > 0 ? timeoutMs : 5000;
        return restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(safeTimeoutMs))
            .setReadTimeout(Duration.ofMillis(safeTimeoutMs))
            .build();
    }

    private String buildUrl() {
        String base = httpBaseUrl.endsWith("/") ? httpBaseUrl.substring(0, httpBaseUrl.length() - 1) : httpBaseUrl;
        String path = httpTransferPath.startsWith("/") ? httpTransferPath : "/" + httpTransferPath;
        return base + path;
    }

    private String extractTransferId(Map<?, ?> body) {
        if (body == null || body.isEmpty()) {
            return "ext_" + UUID.randomUUID().toString().substring(0, 8);
        }

        Object[] candidates = new Object[] {
            body.get("transferId"),
            body.get("id"),
            body.get("transactionId"),
            body.get("payoutId"),
            body.get("reference"),
            body.get("balance_transaction")
        };

        for (Object candidate : candidates) {
            if (candidate instanceof String s && !s.isBlank()) {
                return s;
            }
        }

        return "ext_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String transferViaMock(UUID merchantId, BigDecimal amount, String currency, String reference) {
        simulateProcessingDelay();

        if (ThreadLocalRandom.current().nextInt(100) < Math.max(0, Math.min(100, mockFailureRatePercent))) {
            log.error("Mock bank transfer failed: insufficient funds in platform account");
            throw new RuntimeException("Bank transfer failed: insufficient funds");
        }

        String transferId = "txn_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("Mock bank transfer successful: transferId={}, amount={} {}, merchantId={}, reference={}",
            transferId, amount, currency, merchantId, reference);

        return transferId;
    }

    private void simulateProcessingDelay() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(200, 701));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
