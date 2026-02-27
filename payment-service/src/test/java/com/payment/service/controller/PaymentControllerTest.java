package com.payment.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.service.dto.request.CreatePaymentRequest;
import com.payment.service.dto.request.PaymentMethodRequest;
import com.payment.service.entity.Money;
import com.payment.service.entity.Payment;
import com.payment.service.entity.PaymentStatus;
import com.payment.service.mapper.PaymentMapper;
import com.payment.service.security.JwtUtil;
import com.payment.service.service.PaymentOperationResult;
import com.payment.service.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = PaymentController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private PaymentService paymentService;
    
    @MockBean
    private PaymentMapper paymentMapper;

    @MockBean
    private JwtUtil jwtUtil;
    
    @Test
    void shouldCreatePayment() throws Exception {
        // Given
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .capture(true)
            .paymentMethod(PaymentMethodRequest.builder()
                .type("card")
                .cardToken("tok_visa_4242")
                .build())
            .build();
        
        Payment payment = Payment.builder()
            .id(UUID.randomUUID())
            .amount(Money.of(new BigDecimal("100.00"), "USD"))
            .status(PaymentStatus.CAPTURED)
            .createdAt(Instant.now())
            .build();
        
        when(paymentService.createPayment(any(), anyString(), any(UUID.class)))
            .thenReturn(PaymentOperationResult.of(payment));
        when(paymentMapper.toResponse(any(Payment.class), any())).thenReturn(
            com.payment.service.dto.response.PaymentResponse.builder()
                .id(payment.getId().toString())
                .amount(10000L)
                .currency("usd")
                .status("captured")
                .captured(true)
                .build()
        );
        
        // When & Then
        UUID merchantId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/payments")
                .header("Idempotency-Key", "test_key_123")
                .requestAttr("merchantId", merchantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.amount").value(10000))
            .andExpect(jsonPath("$.currency").value("usd"))
            .andExpect(jsonPath("$.status").value("captured"))
            .andExpect(jsonPath("$.captured").value(true));
    }
    
    @Test
    void shouldReturnBadRequestForInvalidAmount() throws Exception {
        // Given
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .amount(10L) // Too small!
            .currency("USD")
            .paymentMethod(PaymentMethodRequest.builder()
                .type("card")
                .cardToken("tok_visa")
                .build())
            .build();
        
        // When & Then
        mockMvc.perform(post("/api/v1/payments")
                .header("Idempotency-Key", "test_key")
                .requestAttr("merchantId", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.type").value("validation_error"))
            .andExpect(jsonPath("$.error.code").value("invalid_request"));
    }
    
    @Test
    void shouldGetPayment() throws Exception {
        // Given
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
            .id(paymentId)
            .amount(Money.of(new BigDecimal("100.00"), "USD"))
            .status(PaymentStatus.AUTHORIZED)
            .build();
        
        when(paymentService.getPayment(paymentId)).thenReturn(payment);
        when(paymentMapper.toResponse(any())).thenReturn(
            com.payment.service.dto.response.PaymentResponse.builder()
                .id(paymentId.toString())
                .amount(10000L)
                .currency("usd")
                .status("authorized")
                .build()
        );
        
        // When & Then
        mockMvc.perform(get("/api/v1/payments/" + paymentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(paymentId.toString()))
            .andExpect(jsonPath("$.status").value("authorized"));
    }
    
    @Test
    void shouldCapturePayment() throws Exception {
        // Given
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
            .id(paymentId)
            .amount(Money.of(new BigDecimal("100.00"), "USD"))
            .status(PaymentStatus.CAPTURED)
            .build();
        
        when(paymentService.capturePayment(any(UUID.class), any())).thenReturn(payment);
        when(paymentMapper.toResponse(any())).thenReturn(
            com.payment.service.dto.response.PaymentResponse.builder()
                .id(paymentId.toString())
                .status("captured")
                .captured(true)
                .build()
        );
        
        // When & Then
        mockMvc.perform(post("/api/v1/payments/" + paymentId + "/capture"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("captured"))
            .andExpect(jsonPath("$.captured").value(true));
    }
}
