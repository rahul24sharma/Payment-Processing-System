package com.payment.service.controller;

import com.payment.service.entity.StripeWebhookEvent;
import com.payment.service.repository.StripeWebhookEventRepository;
import com.payment.service.security.JwtUtil;
import com.payment.service.service.PaymentService;
import com.payment.service.service.StripePaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = StripeWebhookController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.stripe.webhook-secret=whsec_test")
class StripeWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StripePaymentService stripePaymentService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private StripeWebhookEventRepository stripeWebhookEventRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void shouldRejectWebhookWhenSignatureInvalid() throws Exception {
        String payload = """
            {"id":"evt_invalid","type":"payment_intent.succeeded","data":{"object":{"id":"pi_test_123"}}}
            """;

        when(stripePaymentService.verifyWebhookSignature(eq(payload.trim()), eq("sig"), eq("whsec_test")))
            .thenReturn(false);

        mockMvc.perform(post("/api/v1/webhooks/stripe")
                .header("Stripe-Signature", "sig")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.received").value(false))
            .andExpect(jsonPath("$.error").value("invalid_signature"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldDispatchPaymentIntentSucceededWebhook() throws Exception {
        String payload = """
            {"id":"evt_success","type":"payment_intent.succeeded","data":{"object":{"id":"pi_123"}}}
            """;

        when(stripePaymentService.verifyWebhookSignature(anyString(), eq("sig"), eq("whsec_test")))
            .thenReturn(true);
        when(stripeWebhookEventRepository.save(any(StripeWebhookEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/webhooks/stripe")
                .header("Stripe-Signature", "sig")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.received").value(true))
            .andExpect(jsonPath("$.type").value("payment_intent.succeeded"));

        verify(paymentService).handleStripePaymentIntentSucceededWebhook("pi_123");
    }

    @Test
    void shouldDispatchPaymentIntentFailedWebhook() throws Exception {
        String payload = """
            {
              "id":"evt_fail",
              "type":"payment_intent.payment_failed",
              "data":{
                "object":{
                  "id":"pi_fail_123",
                  "last_payment_error":{
                    "message":"Card declined",
                    "code":"card_declined"
                  }
                }
              }
            }
            """;

        when(stripePaymentService.verifyWebhookSignature(anyString(), eq("sig"), eq("whsec_test")))
            .thenReturn(true);
        when(stripeWebhookEventRepository.save(any(StripeWebhookEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/webhooks/stripe")
                .header("Stripe-Signature", "sig")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.received").value(true))
            .andExpect(jsonPath("$.type").value("payment_intent.payment_failed"));

        verify(paymentService).handleStripePaymentIntentFailedWebhook(
            "pi_fail_123", "Card declined", "card_declined");
    }

    @Test
    void shouldDispatchChargeRefundedWebhook() throws Exception {
        String payload = """
            {
              "id":"evt_refunded",
              "type":"charge.refunded",
              "data":{
                "object":{
                  "payment_intent":"pi_ref_123",
                  "amount_refunded":5000,
                  "currency":"usd"
                }
              }
            }
            """;

        when(stripePaymentService.verifyWebhookSignature(anyString(), eq("sig"), eq("whsec_test")))
            .thenReturn(true);
        when(stripeWebhookEventRepository.save(any(StripeWebhookEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/webhooks/stripe")
                .header("Stripe-Signature", "sig")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.received").value(true))
            .andExpect(jsonPath("$.type").value("charge.refunded"));

        verify(paymentService).handleStripeChargeRefundedWebhook("pi_ref_123", 5000L, "USD");
    }

    @Test
    void shouldDispatchPaymentIntentRequiresCaptureWebhook() throws Exception {
        String payload = """
            {
              "id":"evt_requires_capture",
              "type":"payment_intent.amount_capturable_updated",
              "data":{
                "object":{
                  "id":"pi_cap_123"
                }
              }
            }
            """;

        when(stripePaymentService.verifyWebhookSignature(anyString(), eq("sig"), eq("whsec_test")))
            .thenReturn(true);
        when(stripeWebhookEventRepository.save(any(StripeWebhookEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/webhooks/stripe")
                .header("Stripe-Signature", "sig")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.received").value(true))
            .andExpect(jsonPath("$.type").value("payment_intent.amount_capturable_updated"));

        verify(paymentService).handleStripePaymentIntentRequiresCaptureWebhook("pi_cap_123");
    }

    @Test
    void shouldDispatchPaymentIntentCanceledWebhook() throws Exception {
        String payload = """
            {
              "id":"evt_canceled",
              "type":"payment_intent.canceled",
              "data":{
                "object":{
                  "id":"pi_cancel_123"
                }
              }
            }
            """;

        when(stripePaymentService.verifyWebhookSignature(anyString(), eq("sig"), eq("whsec_test")))
            .thenReturn(true);
        when(stripeWebhookEventRepository.save(any(StripeWebhookEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/webhooks/stripe")
                .header("Stripe-Signature", "sig")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.received").value(true))
            .andExpect(jsonPath("$.type").value("payment_intent.canceled"));

        verify(paymentService).handleStripePaymentIntentCanceledWebhook("pi_cancel_123");
    }

    @Test
    void shouldIgnoreUnsupportedWebhookType() throws Exception {
        String payload = """
            {"id":"evt_unknown","type":"customer.created","data":{"object":{"id":"cus_123"}}}
            """;

        when(stripePaymentService.verifyWebhookSignature(anyString(), eq("sig"), eq("whsec_test")))
            .thenReturn(true);
        when(stripeWebhookEventRepository.save(any(StripeWebhookEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/webhooks/stripe")
                .header("Stripe-Signature", "sig")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.received").value(true))
            .andExpect(jsonPath("$.type").value("customer.created"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldReturnDuplicateResponseForAlreadyProcessedEvent() throws Exception {
        String payload = """
            {"id":"evt_dup","type":"payment_intent.succeeded","data":{"object":{"id":"pi_123"}}}
            """;

        when(stripePaymentService.verifyWebhookSignature(anyString(), eq("sig"), eq("whsec_test")))
            .thenReturn(true);
        when(stripeWebhookEventRepository.save(any(StripeWebhookEvent.class)))
            .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));

        mockMvc.perform(post("/api/v1/webhooks/stripe")
                .header("Stripe-Signature", "sig")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.received").value(true))
            .andExpect(jsonPath("$.duplicate").value(true));

        verifyNoInteractions(paymentService);
    }
}
