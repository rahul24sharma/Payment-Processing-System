package com.payment.service.service;

import com.payment.service.dto.request.CreatePaymentRequest;
import com.payment.service.dto.request.CustomerRequest;
import com.payment.service.dto.request.PaymentMethodRequest;
import com.payment.service.entity.*;
import com.payment.service.exception.InvalidAmountException;
import com.payment.service.exception.InvalidStateTransitionException;
import com.payment.service.exception.PaymentNotFoundException;
import com.payment.service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    
    @Mock
    private PaymentRepository paymentRepository;
    
    @Mock
    private RefundRepository refundRepository;
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private PaymentEventRepository paymentEventRepository;
    
    @Mock
    private IdempotencyService idempotencyService;
    
    @Mock
    private FraudService fraudService;
    
    @Mock
    private MockProcessorService processorService;
    
    @InjectMocks
    private PaymentService paymentService;
    
    @BeforeEach
    void setUp() {
        // Default mock behaviors
        when(idempotencyService.findByKey(anyString())).thenReturn(Optional.empty());
        when(fraudService.assessRisk(any())).thenReturn(BigDecimal.valueOf(15)); // Low risk
        when(processorService.authorize(any())).thenReturn("proc_mock_123");
    }
    
    @Test
    void shouldCreatePaymentSuccessfully() {
        // Given
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .amount(10000L) // $100.00
            .currency("USD")
            .capture(false) // Auth only
            .paymentMethod(PaymentMethodRequest.builder()
                .type("card")
                .cardToken("tok_visa_4242")
                .build())
            .customer(CustomerRequest.builder()
                .email("customer@example.com")
                .name("John Doe")
                .build())
            .build();
        
        Customer customer = Customer.builder()
            .id(UUID.randomUUID())
            .email("customer@example.com")
            .build();
        
        when(customerRepository.findByEmail("customer@example.com"))
            .thenReturn(Optional.of(customer));
        
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Payment result = paymentService.createPayment(request, "test_idempotency_key");
        
        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.AUTHORIZED, result.getStatus());
        assertEquals(new BigDecimal("100.00"), result.getAmount().getAmount());
        assertEquals("USD", result.getAmount().getCurrency());
        assertEquals(BigDecimal.valueOf(15), result.getFraudScore());
        
        verify(fraudService).assessRisk(any());
        verify(processorService).authorize(any());
        verify(paymentRepository, atLeast(1)).save(any());
        verify(idempotencyService).store(eq("test_idempotency_key"), any());
    }
    
    @Test
    void shouldDeclinePaymentWithHighFraudScore() {
        // Given
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .amount(10000L)
            .currency("USD")
            .capture(false)
            .paymentMethod(PaymentMethodRequest.builder()
                .type("card")
                .cardToken("tok_visa_4242")
                .build())
            .build();
        
        Customer customer = Customer.builder()
            .id(UUID.randomUUID())
            .email("anonymous@payment.com")
            .build();
        
        when(customerRepository.save(any())).thenReturn(customer);
        when(fraudService.assessRisk(any())).thenReturn(BigDecimal.valueOf(85)); // High risk!
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        Payment result = paymentService.createPayment(request, "test_key");
        
        // Then
        assertEquals(PaymentStatus.DECLINED, result.getStatus());
        assertNotNull(result.getFailureReason());
        assertTrue(result.getFailureReason().contains("fraud score"));
        
        verify(fraudService).assessRisk(any());
        verify(processorService, never()).authorize(any()); // Should NOT call processor
    }
    
    @Test
    void shouldReturnCachedPaymentForDuplicateIdempotencyKey() {
        // Given
        String idempotencyKey = "duplicate_key";
        Payment cachedPayment = Payment.builder()
            .id(UUID.randomUUID())
            .status(PaymentStatus.AUTHORIZED)
            .amount(Money.of(new BigDecimal("50.00"), "USD"))
            .build();
        
        when(idempotencyService.findByKey(idempotencyKey))
            .thenReturn(Optional.of(cachedPayment));
        
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .amount(5000L)
            .currency("USD")
            .paymentMethod(PaymentMethodRequest.builder()
                .type("card")
                .cardToken("tok_visa")
                .build())
            .build();
        
        // When
        Payment result = paymentService.createPayment(request, idempotencyKey);
        
        // Then
        assertEquals(cachedPayment.getId(), result.getId());
        verify(fraudService, never()).assessRisk(any());
        verify(processorService, never()).authorize(any());
        verify(paymentRepository, never()).save(any());
    }
    
    @Test
    void shouldCaptureAuthorizedPayment() {
        // Given
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
            .id(paymentId)
            .amount(Money.of(new BigDecimal("100.00"), "USD"))
            .status(PaymentStatus.AUTHORIZED)
            .processorPaymentId("proc_123")
            .version(1)
            .build();
        
        when(paymentRepository.findByIdForUpdate(paymentId))
            .thenReturn(Optional.of(payment));
        
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        doNothing().when(processorService).capture(anyString(), any());
        
        // When
        Payment result = paymentService.capturePayment(paymentId, null);
        
        // Then
        assertEquals(PaymentStatus.CAPTURED, result.getStatus());
        assertNotNull(result.getCapturedAt());
        
        verify(processorService).capture(eq("proc_123"), any());
        verify(paymentRepository).save(any());
    }
    
    @Test
    void shouldThrowExceptionWhenCapturingNonAuthorizedPayment() {
        // Given
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
            .id(paymentId)
            .status(PaymentStatus.CAPTURED) // Already captured!
            .build();
        
        when(paymentRepository.findByIdForUpdate(paymentId))
            .thenReturn(Optional.of(payment));
        
        // When & Then
        assertThrows(InvalidStateTransitionException.class, 
            () -> paymentService.capturePayment(paymentId, null));
        
        verify(processorService, never()).capture(anyString(), any());
    }
    
    @Test
    void shouldThrowExceptionForPaymentNotFound() {
        // Given
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findByIdForUpdate(paymentId))
            .thenReturn(Optional.empty());
        
        // When & Then
        assertThrows(PaymentNotFoundException.class, 
            () -> paymentService.capturePayment(paymentId, null));
    }
    
    @Test
    void shouldRejectInvalidAmount() {
        // Given
        CreatePaymentRequest request = CreatePaymentRequest.builder()
            .amount(10L) // Too small! Must be >= 50
            .currency("USD")
            .paymentMethod(PaymentMethodRequest.builder()
                .type("card")
                .cardToken("tok_visa")
                .build())
            .build();
        
        // When & Then
        assertThrows(InvalidAmountException.class, 
            () -> paymentService.createPayment(request, "test_key"));
        
        verify(fraudService, never()).assessRisk(any());
        verify(processorService, never()).authorize(any());
    }
}