package com.payment.service.service;

import com.payment.service.dto.request.CreatePaymentRequest;
import com.payment.service.dto.request.CustomerRequest;
import com.payment.service.dto.request.AddressRequest;
import com.payment.service.dto.request.PaymentMethodRequest;
import com.payment.service.entity.*;
import com.payment.service.exception.InvalidAmountException;
import com.payment.service.exception.InvalidStateTransitionException;
import com.payment.service.exception.PaymentNotFoundException;
import com.payment.service.client.FraudServiceClient;
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
import static org.mockito.ArgumentMatchers.eq;
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
    private FraudServiceClient fraudServiceClient;
    
    @Mock
    private StripePaymentService stripePaymentService;

    @Mock
    private EventPublisher eventPublisher;
    
    @InjectMocks
    private PaymentService paymentService;
    
    @BeforeEach
    void setUp() {
        // Default mock behaviors
        when(idempotencyService.findByKey(anyString())).thenReturn(Optional.empty());
        when(fraudServiceClient.assessRisk(any())).thenReturn(BigDecimal.valueOf(15)); // Low risk
        when(stripePaymentService.authorize(any())).thenReturn("pi_mock_123");
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
                .savedPaymentMethodId("pm_test_123")
                .build())
            .customer(CustomerRequest.builder()
                .email("customer@example.com")
                .name("John Doe")
                .address(AddressRequest.builder()
                    .line1("MG Road 1")
                    .city("Bengaluru")
                    .state("KA")
                    .postalCode("560001")
                    .country("IN")
                    .build())
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
        PaymentOperationResult operationResult =
            paymentService.createPayment(request, "test_idempotency_key", UUID.randomUUID());
        Payment result = operationResult.getPayment();
        
        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.AUTHORIZED, result.getStatus());
        assertEquals(new BigDecimal("100.00"), result.getAmount().getAmount());
        assertEquals("USD", result.getAmount().getCurrency());
        assertEquals(BigDecimal.valueOf(15), result.getFraudScore());
        
        verify(fraudServiceClient).assessRisk(any());
        verify(stripePaymentService).authorize(any());
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
                .savedPaymentMethodId("pm_test_123")
                .build())
            .customer(CustomerRequest.builder()
                .email("risk@example.com")
                .name("Risky User")
                .address(AddressRequest.builder()
                    .line1("Street 1")
                    .city("Mumbai")
                    .state("MH")
                    .postalCode("400001")
                    .country("IN")
                    .build())
                .build())
            .build();
        
        Customer customer = Customer.builder()
            .id(UUID.randomUUID())
            .email("anonymous@payment.com")
            .build();
        
        when(customerRepository.save(any())).thenReturn(customer);
        when(fraudServiceClient.assessRisk(any())).thenReturn(BigDecimal.valueOf(85)); // High risk!
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        PaymentOperationResult operationResult =
            paymentService.createPayment(request, "test_key", UUID.randomUUID());
        Payment result = operationResult.getPayment();

        // Then
        assertEquals(PaymentStatus.DECLINED, result.getStatus());
        assertNotNull(result.getFailureReason());
        assertTrue(result.getFailureReason().contains("fraud score"));
        
        verify(fraudServiceClient).assessRisk(any());
        verify(stripePaymentService, never()).authorize(any()); // Should NOT call processor
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
                .savedPaymentMethodId("pm_test_123")
                .build())
            .build();
        
        // When
        PaymentOperationResult operationResult =
            paymentService.createPayment(request, idempotencyKey, UUID.randomUUID());
        Payment result = operationResult.getPayment();
        
        // Then
        assertEquals(cachedPayment.getId(), result.getId());
        verify(fraudServiceClient, never()).assessRisk(any());
        verify(stripePaymentService, never()).authorize(any());
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
        
        doNothing().when(stripePaymentService).capture(anyString(), any());
        
        // When
        Payment result = paymentService.capturePayment(paymentId, null);
        
        // Then
        assertEquals(PaymentStatus.CAPTURED, result.getStatus());
        assertNotNull(result.getCapturedAt());
        
        verify(stripePaymentService).capture(eq("proc_123"), any());
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
        
        verify(stripePaymentService, never()).capture(anyString(), any());
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
                .savedPaymentMethodId("pm_test_123")
                .build())
            .build();
        
        // When & Then
        assertThrows(InvalidAmountException.class, 
            () -> paymentService.createPayment(request, "test_key", UUID.randomUUID()));
        
        verify(fraudServiceClient, never()).assessRisk(any());
        verify(stripePaymentService, never()).authorize(any());
    }

    @Test
    void webhookSucceededShouldAuthorizeAndCapturePendingPaymentOnce() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
            .id(paymentId)
            .amount(Money.of(new BigDecimal("100.00"), "USD"))
            .status(PaymentStatus.PENDING)
            .processorPaymentId("pi_webhook_123")
            .metadata(new java.util.HashMap<>())
            .version(1)
            .build();

        when(paymentRepository.findByProcessorPaymentId("pi_webhook_123"))
            .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentEventRepository.save(any(PaymentEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.handleStripePaymentIntentSucceededWebhook("pi_webhook_123");

        assertEquals(PaymentStatus.CAPTURED, payment.getStatus());
        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
        verify(paymentEventRepository, times(2)).save(any(PaymentEvent.class)); // AUTHORIZED + CAPTURED
        verify(eventPublisher).publishPaymentEvent(eq("PAYMENT_CAPTURED"), eq(payment), eq("PENDING"));
    }

    @Test
    void repeatedWebhookSucceededShouldBeIgnoredForCapturedPayment() {
        Payment payment = Payment.builder()
            .id(UUID.randomUUID())
            .amount(Money.of(new BigDecimal("100.00"), "USD"))
            .status(PaymentStatus.CAPTURED)
            .processorPaymentId("pi_dup_123")
            .version(1)
            .build();

        when(paymentRepository.findByProcessorPaymentId("pi_dup_123"))
            .thenReturn(Optional.of(payment));

        paymentService.handleStripePaymentIntentSucceededWebhook("pi_dup_123");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentEventRepository, never()).save(any(PaymentEvent.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void failedWebhookAfterCapturedShouldNotDowngradeState() {
        Payment payment = Payment.builder()
            .id(UUID.randomUUID())
            .amount(Money.of(new BigDecimal("100.00"), "USD"))
            .status(PaymentStatus.CAPTURED)
            .processorPaymentId("pi_captured_123")
            .version(1)
            .build();

        when(paymentRepository.findByProcessorPaymentId("pi_captured_123"))
            .thenReturn(Optional.of(payment));

        paymentService.handleStripePaymentIntentFailedWebhook("pi_captured_123", "late failure", "late_failure");

        assertEquals(PaymentStatus.CAPTURED, payment.getStatus());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(paymentEventRepository, never()).save(any(PaymentEvent.class));
    }

    @Test
    void chargeRefundedWebhookShouldBeIdempotentForAlreadyRefundedPayment() {
        Payment payment = Payment.builder()
            .id(UUID.randomUUID())
            .amount(Money.of(new BigDecimal("100.00"), "USD"))
            .status(PaymentStatus.CAPTURED)
            .processorPaymentId("pi_ref_123")
            .version(1)
            .build();

        when(paymentRepository.findByProcessorPaymentId("pi_ref_123"))
            .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentEventRepository.save(any(PaymentEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.handleStripeChargeRefundedWebhook("pi_ref_123", 10_000L, "USD");
        paymentService.handleStripeChargeRefundedWebhook("pi_ref_123", 10_000L, "USD");

        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        verify(paymentEventRepository, times(1)).save(any(PaymentEvent.class)); // no duplicate refund event
    }

    @Test
    void refundUpdatedSucceededShouldIgnoreOutOfOrderForNonCapturedPaymentThenApplyLater() {
        Payment payment = Payment.builder()
            .id(UUID.randomUUID())
            .amount(Money.of(new BigDecimal("100.00"), "USD"))
            .status(PaymentStatus.PENDING)
            .processorPaymentId("pi_refupd_123")
            .version(1)
            .build();

        when(paymentRepository.findByProcessorPaymentId("pi_refupd_123"))
            .thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentEventRepository.save(any(PaymentEvent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.handleStripeRefundWebhook("pi_refupd_123", "succeeded", 5_000L, "USD");
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        verify(paymentRepository, never()).save(any(Payment.class));

        payment.setStatus(PaymentStatus.CAPTURED);
        paymentService.handleStripeRefundWebhook("pi_refupd_123", "succeeded", 5_000L, "USD");

        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, payment.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentEventRepository, times(1)).save(any(PaymentEvent.class));
    }
}
