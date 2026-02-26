package com.payment.service.service;

import com.payment.service.entity.Money;
import com.payment.service.entity.Payment;
import com.payment.service.exception.PaymentActionRequiredException;
import com.payment.service.exception.ProcessorException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Stripe payment processor integration
 */
@Service
@Primary
@Slf4j
public class StripePaymentService {
    
    @Value("${spring.stripe.api-key:${stripe.api-key:}}")
    private String stripeApiKey;
    
    @PostConstruct
    public void init() {
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            log.warn("Stripe API key is not configured. Stripe operations will fail until it is set.");
            return;
        }
        Stripe.apiKey = stripeApiKey;
        log.info("Stripe API initialized");
    }
    
    /**
     * Create and authorize payment with Stripe
     */
    public String authorize(Payment payment) {
        ensureConfigured();
        log.info("Authorizing payment with Stripe: paymentId={}, amount={}", 
            payment.getId(), payment.getAmount());
        
        try {
            String stripePaymentMethodId = getMetadataString(payment.getMetadata(), "stripe_payment_method_id");
            if (stripePaymentMethodId == null || stripePaymentMethodId.isBlank()) {
                throw new ProcessorException(
                    "Stripe payment method is required",
                    "stripe",
                    "missing_payment_method"
                );
            }

            // Build payment intent parameters
            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(payment.getAmount().getAmountInCents())
                .setCurrency(payment.getAmount().getCurrency().toLowerCase())
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL) // Authorize only
                .setConfirm(true)  // Confirm immediately
                .setPaymentMethod(stripePaymentMethodId)
                .putMetadata("payment_id", payment.getId().toString())
                .putMetadata("merchant_id", payment.getMerchantId().toString())
                .setDescription("Payment via Payment System");

            enrichIndiaExportFields(paramsBuilder, payment);
            PaymentIntentCreateParams params = paramsBuilder.build();
            
            // Create payment intent
            PaymentIntent intent = PaymentIntent.create(params);
            
            log.info("Stripe payment authorized: intentId={}, status={}", 
                intent.getId(), intent.getStatus());
            
            // Check if requires action (3D Secure, etc.)
            if ("requires_action".equals(intent.getStatus())) {
                throw new PaymentActionRequiredException(
                    "Payment requires additional authentication",
                    intent.getId(),
                    intent.getClientSecret(),
                    intent.getNextAction() != null ? intent.getNextAction().getType() : "use_stripe_sdk"
                );
            }
            
            if (!"requires_capture".equals(intent.getStatus())) {
                throw new ProcessorException(
                    "Payment authorization failed: " + intent.getStatus(),
                    "stripe",
                    intent.getStatus()
                );
            }
            
            return intent.getId();
            
        } catch (StripeException e) {
            log.error("Stripe authorization failed: code={}, message={}", 
                e.getCode(), e.getMessage());
            
            throw new ProcessorException(
                "Stripe error: " + e.getMessage(),
                "stripe",
                e.getCode() != null ? e.getCode() : "stripe_error",
                e
            );
        }
    }
    
    /**
     * Capture an authorized payment
     */
    public void capture(String stripePaymentIntentId, Money amount) {
        ensureConfigured();
        log.info("Capturing payment with Stripe: intentId={}, amount={}", 
            stripePaymentIntentId, amount);
        
        try {
            PaymentIntent intent = PaymentIntent.retrieve(stripePaymentIntentId);
            
            PaymentIntentCaptureParams params = PaymentIntentCaptureParams.builder()
                .setAmountToCapture(amount.getAmountInCents())
                .build();
            
            PaymentIntent captured = intent.capture(params);
            
            log.info("Stripe payment captured: intentId={}, status={}", 
                captured.getId(), captured.getStatus());
            
            if (!"succeeded".equals(captured.getStatus())) {
                throw new ProcessorException(
                    "Capture failed: " + captured.getStatus(),
                    "stripe",
                    captured.getStatus()
                );
            }
            
        } catch (StripeException e) {
            log.error("Stripe capture failed: code={}, message={}", 
                e.getCode(), e.getMessage());
            
            throw new ProcessorException(
                "Stripe capture error: " + e.getMessage(),
                "stripe",
                e.getCode() != null ? e.getCode() : "capture_failed",
                e
            );
        }
    }
    
    /**
     * Cancel (void) an authorized payment
     */
    public void voidAuthorization(String stripePaymentIntentId) {
        ensureConfigured();
        log.info("Voiding Stripe payment: intentId={}", stripePaymentIntentId);
        
        try {
            PaymentIntent intent = PaymentIntent.retrieve(stripePaymentIntentId);
            PaymentIntent canceled = intent.cancel();
            
            log.info("Stripe payment voided: intentId={}, status={}", 
                canceled.getId(), canceled.getStatus());
            
        } catch (StripeException e) {
            log.error("Stripe void failed: code={}, message={}", 
                e.getCode(), e.getMessage());
            
            throw new ProcessorException(
                "Stripe void error: " + e.getMessage(),
                "stripe",
                e.getCode() != null ? e.getCode() : "void_failed",
                e
            );
        }
    }
    
    /**
     * Create a refund
     */
    public String refund(String stripePaymentIntentId, Money refundAmount) {
        ensureConfigured();
        log.info("Creating Stripe refund: intentId={}, amount={}", 
            stripePaymentIntentId, refundAmount);
        
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(stripePaymentIntentId)
                .setAmount(refundAmount.getAmountInCents())
                .build();
            
            Refund refund = Refund.create(params);
            
            log.info("Stripe refund created: refundId={}, status={}", 
                refund.getId(), refund.getStatus());
            
            if (!"succeeded".equals(refund.getStatus())) {
                throw new ProcessorException(
                    "Refund failed: " + refund.getStatus(),
                    "stripe",
                    refund.getStatus()
                );
            }
            
            return refund.getId();
            
        } catch (StripeException e) {
            log.error("Stripe refund failed: code={}, message={}", 
                e.getCode(), e.getMessage());
            
            throw new ProcessorException(
                "Stripe refund error: " + e.getMessage(),
                "stripe",
                e.getCode() != null ? e.getCode() : "refund_failed",
                e
            );
        }
    }
    
    /**
     * Verify Stripe webhook signature
     */
    public boolean verifyWebhookSignature(String payload, String signature, String secret) {
        ensureConfigured();
        try {
            com.stripe.net.Webhook.constructEvent(payload, signature, secret);
            return true;
        } catch (Exception e) {
            log.error("Stripe webhook signature verification failed", e);
            return false;
        }
    }
    
    /**
     * Health check
     */
    public boolean healthCheck() {
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            return false;
        }
        try {
            // Try to retrieve account info
            com.stripe.model.Account account = com.stripe.model.Account.retrieve();
            log.debug("Stripe health check passed: accountId={}", account.getId());
            return true;
        } catch (StripeException e) {
            log.error("Stripe health check failed", e);
            return false;
        }
    }

    public PaymentIntent retrievePaymentIntent(String stripePaymentIntentId) {
        ensureConfigured();
        try {
            return PaymentIntent.retrieve(stripePaymentIntentId);
        } catch (StripeException e) {
            throw new ProcessorException(
                "Stripe retrieve payment intent error: " + e.getMessage(),
                "stripe",
                e.getCode() != null ? e.getCode() : "retrieve_payment_intent_failed",
                e
            );
        }
    }

    private void ensureConfigured() {
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            throw new ProcessorException(
                "Stripe API key is not configured",
                "stripe",
                "stripe_not_configured"
            );
        }
    }

    private void enrichIndiaExportFields(PaymentIntentCreateParams.Builder builder, Payment payment) {
        Map<String, Object> metadata = payment.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return;
        }

        String customerName = getMetadataString(metadata, "stripe_customer_name");
        String customerEmail = getMetadataString(metadata, "stripe_customer_email");
        String line1 = getMetadataString(metadata, "stripe_customer_address_line1");
        String city = getMetadataString(metadata, "stripe_customer_address_city");
        String state = getMetadataString(metadata, "stripe_customer_address_state");
        String postalCode = getMetadataString(metadata, "stripe_customer_address_postal_code");
        String country = getMetadataString(metadata, "stripe_customer_address_country");

        if (isBlank(customerName) || isBlank(line1) || isBlank(city) || isBlank(state)
            || isBlank(postalCode) || isBlank(country)) {
            return;
        }
        if (!isBlank(customerEmail)) {
            builder.setReceiptEmail(customerEmail);
        }

        PaymentIntentCreateParams.Shipping.Address.Builder addressBuilder =
            PaymentIntentCreateParams.Shipping.Address.builder()
                .setLine1(line1)
                .setCity(city)
                .setState(state)
                .setPostalCode(postalCode)
                .setCountry(country);

        String line2 = getMetadataString(metadata, "stripe_customer_address_line2");
        if (!isBlank(line2)) {
            addressBuilder.setLine2(line2);
        }

        builder.setShipping(
            PaymentIntentCreateParams.Shipping.builder()
                .setName(customerName)
                .setAddress(addressBuilder.build())
                .build()
        );
    }

    private String getMetadataString(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
