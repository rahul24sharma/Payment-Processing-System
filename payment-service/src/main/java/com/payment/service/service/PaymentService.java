package com.payment.service.service;

import com.payment.service.dto.request.CapturePaymentRequest;
import com.payment.service.dto.request.CreatePaymentRequest;
import com.payment.service.dto.request.RefundRequest;
import com.payment.service.entity.*;
import com.payment.service.exception.*;
import com.payment.service.repository.CustomerRepository;
import com.payment.service.repository.PaymentEventRepository;
import com.payment.service.repository.PaymentRepository;
import com.payment.service.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final CustomerRepository customerRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final IdempotencyService idempotencyService;
    private final FraudService fraudService;
    private final MockProcessorService processorService;
    
    /**
     * Creates a new payment with idempotency guarantee
     */
    public Payment createPayment(CreatePaymentRequest request, String idempotencyKey) {
        log.info("Creating payment: amount={}, currency={}, idempotencyKey={}", 
            request.getAmount(), request.getCurrency(), idempotencyKey);
        
        // 1. Check idempotency (prevent duplicate payments)
        Optional<Payment> existing = idempotencyService.findByKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Returning cached payment for idempotency key: {}", idempotencyKey);
            return existing.get();
        }
        
        // 2. Validate request
        validateCreateRequest(request);
        
        // 3. Get or create customer
        Customer customer = getOrCreateCustomer(request.getCustomer());
        
        // 4. Create payment entity
        Payment payment = Payment.builder()
            .merchantId(UUID.randomUUID()) // TODO: Get from JWT token
            .customerId(customer.getId())
            .amount(Money.of(request.getAmount(), request.getCurrency()))
            .status(PaymentStatus.PENDING)
            .idempotencyKey(idempotencyKey)
            .metadata(request.getMetadata() != null ? request.getMetadata() : new HashMap<>())
            .createdAt(Instant.now())
            .build();
        
        // 5. Save payment (status = PENDING)
        payment = paymentRepository.save(payment);
        recordEvent(payment, "PAYMENT_CREATED", null, PaymentStatus.PENDING.name());
        
        // 6. Assess fraud risk (call Fraud Service)
        BigDecimal fraudScore = fraudService.assessRisk(payment);
        payment.setFraudScore(fraudScore);
        
        // 7. Check fraud threshold
        if (fraudScore.compareTo(BigDecimal.valueOf(50)) > 0) {
            log.warn("Payment declined due to high fraud score: score={}, paymentId={}", 
                fraudScore, payment.getId());
            
            payment.markDeclined("High fraud score: " + fraudScore);
            payment = paymentRepository.save(payment);
            recordEvent(payment, "PAYMENT_DECLINED", PaymentStatus.PENDING.name(), 
                PaymentStatus.DECLINED.name());
            
            return payment;
        }
        
        // 8. Authorize payment with processor
        try {
            String processorPaymentId = processorService.authorize(payment);
            
            payment.setProcessor("mock_processor");
            payment.setProcessorPaymentId(processorPaymentId);
            payment.authorize();
            payment = paymentRepository.save(payment);
            
            recordEvent(payment, "PAYMENT_AUTHORIZED", PaymentStatus.PENDING.name(), 
                PaymentStatus.AUTHORIZED.name());
            
            log.info("Payment authorized: paymentId={}, processorId={}", 
                payment.getId(), processorPaymentId);
            
        } catch (ProcessorException e) {
            log.error("Payment authorization failed: paymentId={}", payment.getId(), e);
            
            payment.markFailed(e.getMessage(), e.getErrorCode());
            payment = paymentRepository.save(payment);
            recordEvent(payment, "PAYMENT_FAILED", PaymentStatus.PENDING.name(), 
                PaymentStatus.FAILED.name());
            
            throw e;
        }
        
        // 9. Auto-capture if requested
        if (Boolean.TRUE.equals(request.getCapture())) {
            payment = capturePayment(payment.getId(), null);
        }
        
        // 10. Store in idempotency cache
        idempotencyService.store(idempotencyKey, payment);
        
        return payment;
    }
    
    /**
     * Captures an authorized payment
     */
    public Payment capturePayment(UUID paymentId, CapturePaymentRequest request) {
        log.info("Capturing payment: paymentId={}", paymentId);
        
        // 1. Load payment with pessimistic lock (prevent concurrent captures)
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        
        // 2. Validate state transition
        if (!payment.canTransitionTo(PaymentStatus.CAPTURED)) {
            throw new InvalidStateTransitionException(
                payment.getStatus().name(), 
                PaymentStatus.CAPTURED.name()
            );
        }
        
        // 3. Determine capture amount
        Money captureAmount = payment.getAmount();
        if (request != null && request.getAmount() != null) {
            captureAmount = Money.of(request.getAmount(), payment.getAmount().getCurrency());
            
            // Validate capture amount doesn't exceed authorized amount
            if (captureAmount.isGreaterThan(payment.getAmount())) {
                throw new InvalidAmountException(
                    "Capture amount cannot exceed authorized amount");
            }
        }
        
        // 4. Capture with processor
        try {
            processorService.capture(payment.getProcessorPaymentId(), captureAmount);
            
            PaymentStatus previousStatus = payment.getStatus();
            payment.capture();
            payment = paymentRepository.save(payment);
            
            recordEvent(payment, "PAYMENT_CAPTURED", previousStatus.name(), 
                PaymentStatus.CAPTURED.name());
            
            log.info("Payment captured successfully: paymentId={}", paymentId);
            
            return payment;
            
        } catch (ProcessorException e) {
            log.error("Payment capture failed: paymentId={}", paymentId, e);
            throw e;
        }
    }
    
    /**
     * Voids an authorized payment
     */
    public Payment voidPayment(UUID paymentId) {
        log.info("Voiding payment: paymentId={}", paymentId);
        
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        
        if (!payment.canTransitionTo(PaymentStatus.VOID)) {
            throw new InvalidStateTransitionException(
                payment.getStatus().name(), 
                PaymentStatus.VOID.name()
            );
        }
        
        try {
            processorService.voidAuthorization(payment.getProcessorPaymentId());
            
            PaymentStatus previousStatus = payment.getStatus();
            payment.voidPayment();
            payment = paymentRepository.save(payment);
            
            recordEvent(payment, "PAYMENT_VOIDED", previousStatus.name(), 
                PaymentStatus.VOID.name());
            
            log.info("Payment voided successfully: paymentId={}", paymentId);
            
            return payment;
            
        } catch (ProcessorException e) {
            log.error("Payment void failed: paymentId={}", paymentId, e);
            throw e;
        }
    }
    
    /**
     * Refunds a captured payment (full or partial)
     */
    public Refund refundPayment(UUID paymentId, RefundRequest request) {
        log.info("Creating refund: paymentId={}, amount={}", paymentId, request.getAmount());
        
        // 1. Load payment with lock
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        
        // 2. Validate payment can be refunded
        if (payment.getStatus() != PaymentStatus.CAPTURED && 
            payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new InvalidStateTransitionException(
                payment.getStatus().name(), 
                "REFUNDED"
            );
        }
        
        // 3. Determine refund amount
        Money refundAmount;
        if (request.getAmount() != null) {
            refundAmount = Money.of(request.getAmount(), payment.getAmount().getCurrency());
        } else {
            // Full refund
            refundAmount = payment.getRemainingRefundableAmount();
        }
        
        // 4. Validate refund amount
        Money remainingRefundable = payment.getRemainingRefundableAmount();
        if (refundAmount.isGreaterThan(remainingRefundable)) {
            throw new InvalidAmountException(
                String.format("Refund amount %s exceeds remaining refundable amount %s",
                    refundAmount, remainingRefundable)
            );
        }
        
        // 5. Create refund entity
        Refund refund = Refund.builder()
            .id(UUID.randomUUID())
            .payment(payment)
            .amount(refundAmount)
            .reason(request.getReason())
            .status(RefundStatus.PENDING)
            .createdAt(Instant.now())
            .build();
        
        // 6. Process refund with processor
        try {
            String processorRefundId = processorService.refund(
                payment.getProcessorPaymentId(), 
                refundAmount
            );
            
            refund.markSucceeded(processorRefundId);
            refund = refundRepository.save(refund);
            
            // Update payment status
            PaymentStatus previousStatus = payment.getStatus();
            payment.addRefund(refund);
            payment = paymentRepository.save(payment);
            
            recordEvent(payment, "PAYMENT_REFUNDED", previousStatus.name(), 
                payment.getStatus().name());
            
            log.info("Refund created successfully: refundId={}, paymentId={}", 
                refund.getId(), paymentId);
            
            return refund;
            
        } catch (ProcessorException e) {
            log.error("Refund processing failed: paymentId={}", paymentId, e);
            
            refund.markFailed(e.getMessage());
            refundRepository.save(refund);
            
            throw e;
        }
    }
    
    /**
     * Get payment by ID
     */
    @Transactional(readOnly = true)
    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
    
    /**
     * List payments by merchant
     */
    @Transactional(readOnly = true)
    public List<Payment> listPayments(UUID merchantId, PaymentStatus status, int limit) {
        if (status != null) {
            return paymentRepository.findByMerchantAndStatuses(
                merchantId, 
                Collections.singletonList(status)
            );
        }
        
        return paymentRepository.findByMerchantId(merchantId);
    }
    
    /**
     * Get or create customer
     */
    private Customer getOrCreateCustomer(
            com.payment.service.dto.request.CustomerRequest customerRequest) {
        
        if (customerRequest == null || customerRequest.getEmail() == null) {
            // Create anonymous customer
            return customerRepository.save(Customer.builder()
                .email("anonymous-" + UUID.randomUUID() + "@payment.com")
                .createdAt(Instant.now())
                .build());
        }

        // Check if customer exists
        Optional<Customer> existing = customerRepository.findByEmail(customerRequest.getEmail());
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new customer
        return customerRepository.save(Customer.builder()
            .email(customerRequest.getEmail())
            .name(customerRequest.getName())
            .phone(customerRequest.getPhone())
            .createdAt(Instant.now())
            .build());
    }
    
    /**
     * Validate payment creation request
     */
    private void validateCreateRequest(CreatePaymentRequest request) {
        if (request.getAmount() == null || request.getAmount() < 50) {
            throw new InvalidAmountException("Amount must be at least 50 cents");
        }
        
        if (request.getCurrency() == null || request.getCurrency().length() != 3) {
            throw new ValidationException("Invalid currency code");
        }
        
        if (request.getPaymentMethod() == null) {
            throw new ValidationException("Payment method is required");
        }
    }
    
    /**
     * Record payment event for audit trail
     */
    private void recordEvent(Payment payment, String eventType, 
                            String previousState, String newState) {
        PaymentEvent event = PaymentEvent.builder()
            .paymentId(payment.getId())
            .eventType(eventType)
            .previousState(previousState)
            .newState(newState)
            .createdAt(Instant.now())
            .createdBy("SYSTEM")
            .build();
        
        paymentEventRepository.save(event);
    }
}