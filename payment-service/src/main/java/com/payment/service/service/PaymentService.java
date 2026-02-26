package com.payment.service.service;

import com.payment.service.client.FraudServiceClient;
import com.payment.service.dto.request.CapturePaymentRequest;
import com.payment.service.dto.request.CreatePaymentRequest;
import com.payment.service.dto.request.RefundRequest;
import com.payment.service.dto.request.CustomerRequest;
import com.payment.service.dto.request.AddressRequest;
import com.payment.service.dto.request.PaymentMethodRequest;
import com.payment.service.dto.response.PaymentNextActionResponse;
import com.payment.service.entity.*;
import com.payment.service.exception.*;
import com.stripe.model.PaymentIntent;
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
    private final FraudServiceClient fraudServiceClient; 
    // private final FraudService fraudService;
    private final StripePaymentService stripePaymentService;
    private final EventPublisher eventPublisher;
    /**
     * Creates a new payment with idempotency guarantee
     */
    public PaymentOperationResult createPayment(CreatePaymentRequest request, String idempotencyKey,UUID merchantId) {
        log.info("Creating payment: amount={}, currency={}, idempotencyKey={}", 
            request.getAmount(), request.getCurrency(), idempotencyKey);
        
        // 1. Check idempotency (prevent duplicate payments)
        Optional<Payment> existing = idempotencyService.findByKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Returning cached payment for idempotency key: {}", idempotencyKey);
            return PaymentOperationResult.of(existing.get());
        }
        
        // 2. Validate request
        validateCreateRequest(request);
        
        // 3. Get or create customer
        Customer customer = getOrCreateCustomer(request.getCustomer());
        
        // 4. Create payment entity
        Map<String, Object> metadata = request.getMetadata() != null
            ? new HashMap<>(request.getMetadata())
            : new HashMap<>();
        metadata.put("auto_capture_requested", Boolean.TRUE.equals(request.getCapture()));
        enrichStripeCustomerMetadata(metadata, request.getCustomer());
        enrichStripePaymentMethodMetadata(metadata, request.getPaymentMethod());

        Payment payment = Payment.builder()
            .merchantId(merchantId) // From JWT token
            .customerId(customer.getId())
            .amount(Money.of(request.getAmount(), request.getCurrency()))
            .status(PaymentStatus.PENDING)
            .idempotencyKey(idempotencyKey)
            .metadata(metadata)
            .createdAt(Instant.now())
            .build();
        
        // 5. Save payment (status = PENDING)
        payment = paymentRepository.save(payment);
        recordEvent(payment, "PAYMENT_CREATED", null, PaymentStatus.PENDING.name());
        
        // 6. Assess fraud risk (call Fraud Service)
        BigDecimal fraudScore = fraudServiceClient.assessRisk(payment);
        payment.setFraudScore(fraudScore);
        
        // 7. Check fraud threshold
        if (fraudScore.compareTo(BigDecimal.valueOf(50)) > 0) {
            log.warn("Payment declined due to high fraud score: score={}, paymentId={}", 
                fraudScore, payment.getId());
            
            payment.markDeclined("High fraud score: " + fraudScore);
            payment = paymentRepository.save(payment);
            recordEvent(payment, "PAYMENT_DECLINED", PaymentStatus.PENDING.name(), 
                PaymentStatus.DECLINED.name());
            
            return PaymentOperationResult.of(payment);
        }
        
        // 8. Authorize payment with processor
        PaymentNextActionResponse nextAction = null;
        try {
            String stripePaymentIntentId = stripePaymentService.authorize(payment);
            
            payment.setProcessor("stripe");
            payment.setProcessorPaymentId(stripePaymentIntentId);
            payment.authorize();
            payment = paymentRepository.save(payment);

            
            recordEvent(payment, "PAYMENT_AUTHORIZED", PaymentStatus.PENDING.name(), 
                PaymentStatus.AUTHORIZED.name());
            
            log.info("Payment authorized: paymentId={}, processorId={}", 
                payment.getId(), stripePaymentIntentId);
            
        } catch (PaymentActionRequiredException e) {
            log.info("Payment requires customer authentication: paymentId={}, intentId={}",
                payment.getId(), e.getPaymentIntentId());

            payment.setProcessor("stripe");
            payment.setProcessorPaymentId(e.getPaymentIntentId());
            payment.setFailureReason(null);
            payment.setFailureCode(null);
            payment = paymentRepository.save(payment);

            recordEvent(payment, "PAYMENT_AUTHENTICATION_REQUIRED", PaymentStatus.PENDING.name(),
                PaymentStatus.PENDING.name());

            nextAction = PaymentNextActionResponse.builder()
                .type("use_stripe_sdk")
                .clientSecret(e.getClientSecret())
                .paymentIntentId(e.getPaymentIntentId())
                .processor("stripe")
                .status("requires_action")
                .build();
        } catch (ProcessorException e) {
            log.error("Payment authorization failed: paymentId={}", payment.getId(), e);
            
            payment.markFailed(e.getMessage(), e.getErrorCode());
            payment = paymentRepository.save(payment);
            recordEvent(payment, "PAYMENT_FAILED", PaymentStatus.PENDING.name(), 
                PaymentStatus.FAILED.name());
            
            throw e;
        }
        if (payment.getStatus() == PaymentStatus.AUTHORIZED) {
            eventPublisher.publishPaymentEvent("PAYMENT_AUTHORIZED", payment, "PENDING");
        }

        
        // 9. Auto-capture if requested
        if (nextAction == null && Boolean.TRUE.equals(request.getCapture())) {
            payment = capturePayment(payment.getId(), null);
        }

        
        // 10. Store in idempotency cache
        idempotencyService.store(idempotencyKey, payment);
        
        return nextAction != null
            ? PaymentOperationResult.withNextAction(payment, nextAction)
            : PaymentOperationResult.of(payment);
    }
    
    /**
     * Authorizes a pending payment
     */
    public PaymentOperationResult authorizePayment(UUID paymentId) {
        log.info("Authorizing payment: paymentId={}", paymentId);

        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (!payment.canTransitionTo(PaymentStatus.AUTHORIZED)) {
            throw new InvalidStateTransitionException(
                payment.getStatus().name(),
                PaymentStatus.AUTHORIZED.name()
            );
        }

        try {
            String processorPaymentId = stripePaymentService.authorize(payment);

            payment.setProcessor("stripe");
            payment.setProcessorPaymentId(processorPaymentId);

            PaymentStatus previousStatus = payment.getStatus();
            payment.authorize();
            payment = paymentRepository.save(payment);

            recordEvent(payment, "PAYMENT_AUTHORIZED", previousStatus.name(),
                PaymentStatus.AUTHORIZED.name());
            eventPublisher.publishPaymentEvent("PAYMENT_AUTHORIZED", payment, previousStatus.name());

            log.info("Payment authorized successfully: paymentId={}, processorId={}",
                payment.getId(), processorPaymentId);

            return PaymentOperationResult.of(payment);
        } catch (PaymentActionRequiredException e) {
            payment.setProcessor("stripe");
            payment.setProcessorPaymentId(e.getPaymentIntentId());
            payment = paymentRepository.save(payment);
            recordEvent(payment, "PAYMENT_AUTHENTICATION_REQUIRED", payment.getStatus().name(),
                payment.getStatus().name());

            return PaymentOperationResult.withNextAction(
                payment,
                PaymentNextActionResponse.builder()
                    .type("use_stripe_sdk")
                    .clientSecret(e.getClientSecret())
                    .paymentIntentId(e.getPaymentIntentId())
                    .processor("stripe")
                    .status("requires_action")
                    .build()
            );
        } catch (ProcessorException e) {
            log.error("Payment authorization failed: paymentId={}", paymentId, e);
            throw e;
        }
    }

    public PaymentOperationResult completeAuthentication(UUID paymentId) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getProcessorPaymentId() == null || payment.getProcessorPaymentId().isBlank()) {
            throw new ValidationException("Payment has no processor payment intent to confirm");
        }

        PaymentIntent intent = stripePaymentService.retrievePaymentIntent(payment.getProcessorPaymentId());
        String stripeStatus = intent.getStatus();
        log.info("Completing authentication for paymentId={}, intentId={}, stripeStatus={}",
            payment.getId(), intent.getId(), stripeStatus);

        if ("requires_action".equals(stripeStatus)) {
            return PaymentOperationResult.withNextAction(
                payment,
                PaymentNextActionResponse.builder()
                    .type("use_stripe_sdk")
                    .clientSecret(intent.getClientSecret())
                    .paymentIntentId(intent.getId())
                    .processor("stripe")
                    .status(stripeStatus)
                    .build()
            );
        }

        if ("requires_capture".equals(stripeStatus)) {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setProcessor("stripe");
                payment.setFailureReason(null);
                payment.setFailureCode(null);
                payment.authorize();
                payment = paymentRepository.save(payment);
                recordEvent(payment, "PAYMENT_AUTHORIZED", PaymentStatus.PENDING.name(),
                    PaymentStatus.AUTHORIZED.name());
                eventPublisher.publishPaymentEvent("PAYMENT_AUTHORIZED", payment, "PENDING");
            }

            if (Boolean.TRUE.equals(getMetadataBoolean(payment, "auto_capture_requested"))) {
                payment = capturePayment(payment.getId(), null);
            }

            return PaymentOperationResult.of(payment);
        }

        if ("succeeded".equals(stripeStatus)) {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setProcessor("stripe");
                payment.setFailureReason(null);
                payment.setFailureCode(null);
                payment.authorize();
                payment = paymentRepository.save(payment);
                recordEvent(payment, "PAYMENT_AUTHORIZED", PaymentStatus.PENDING.name(),
                    PaymentStatus.AUTHORIZED.name());
                eventPublisher.publishPaymentEvent("PAYMENT_AUTHORIZED", payment, "PENDING");
            }
            if (payment.getStatus() == PaymentStatus.AUTHORIZED) {
                payment = capturePayment(payment.getId(), null);
            }
            return PaymentOperationResult.of(payment);
        }

        if ("canceled".equals(stripeStatus)) {
            payment.markFailed("Stripe payment was canceled during authentication", "canceled");
        } else {
            payment.markFailed("Stripe authorization failed: " + stripeStatus, stripeStatus);
        }
        payment = paymentRepository.save(payment);
        recordEvent(payment, "PAYMENT_FAILED", PaymentStatus.PENDING.name(), PaymentStatus.FAILED.name());
        throw new ProcessorException(
            "Payment authorization failed: " + stripeStatus,
            "stripe",
            stripeStatus
        );
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
            stripePaymentService.capture(payment.getProcessorPaymentId(), captureAmount);

            PaymentStatus previousStatus = payment.getStatus();
            payment.capture();
            payment = paymentRepository.save(payment);

            recordEvent(payment, "PAYMENT_CAPTURED", previousStatus.name(),
                PaymentStatus.CAPTURED.name());

            eventPublisher.publishPaymentEvent("PAYMENT_CAPTURED", payment, previousStatus.name());

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
            stripePaymentService.voidAuthorization(payment.getProcessorPaymentId());
            
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
            .payment(payment)
            .amount(refundAmount)
            .reason(request.getReason())
            .status(RefundStatus.PENDING)
            .createdAt(Instant.now())
            .build();

        // Persist the pending refund before calling Stripe so both success/failure paths
        // update an existing row (avoids stale-state/optimistic-lock style errors).
        refund = refundRepository.save(refund);
        
        // 6. Process refund with processor
        try {
            String processorRefundId = stripePaymentService.refund(
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

    public void handleStripePaymentIntentSucceededWebhook(String paymentIntentId) {
        paymentRepository.findByProcessorPaymentId(paymentIntentId).ifPresentOrElse(payment -> {
            PaymentStatus previousStatus = payment.getStatus();
            if (previousStatus == PaymentStatus.CAPTURED
                || previousStatus == PaymentStatus.REFUNDED
                || previousStatus == PaymentStatus.PARTIALLY_REFUNDED) {
                log.info("Webhook payment_intent.succeeded ignored (already finalized): paymentId={}, status={}",
                    payment.getId(), previousStatus);
                return;
            }

            if (previousStatus == PaymentStatus.PENDING) {
                payment.setProcessor("stripe");
                payment.setFailureReason(null);
                payment.setFailureCode(null);
                payment.authorize();
                recordEvent(payment, "PAYMENT_AUTHORIZED", PaymentStatus.PENDING.name(),
                    PaymentStatus.AUTHORIZED.name());
            }

            if (payment.getStatus() == PaymentStatus.AUTHORIZED) {
                payment.capture();
                paymentRepository.save(payment);
                recordEvent(payment, "PAYMENT_CAPTURED",
                    previousStatus == PaymentStatus.PENDING ? PaymentStatus.AUTHORIZED.name() : previousStatus.name(),
                    PaymentStatus.CAPTURED.name());
                safePublishPaymentEvent("PAYMENT_CAPTURED", payment, previousStatus.name());
            } else {
                paymentRepository.save(payment);
            }
        }, () -> log.warn("Webhook payment_intent.succeeded ignored: no local payment for intentId={}", paymentIntentId));
    }

    public void handleStripePaymentIntentFailedWebhook(String paymentIntentId, String reason, String code) {
        paymentRepository.findByProcessorPaymentId(paymentIntentId).ifPresentOrElse(payment -> {
            PaymentStatus previousStatus = payment.getStatus();
            if (previousStatus == PaymentStatus.CAPTURED
                || previousStatus == PaymentStatus.REFUNDED
                || previousStatus == PaymentStatus.PARTIALLY_REFUNDED) {
                log.info("Webhook payment_intent.payment_failed ignored for finalized payment: paymentId={}, status={}",
                    payment.getId(), previousStatus);
                return;
            }

            payment.markFailed(reason != null ? reason : "Stripe payment failed", code != null ? code : "payment_failed");
            paymentRepository.save(payment);
            recordEvent(payment, "PAYMENT_FAILED", previousStatus.name(), PaymentStatus.FAILED.name());
        }, () -> log.warn("Webhook payment_intent.payment_failed ignored: no local payment for intentId={}", paymentIntentId));
    }

    public void handleStripePaymentIntentRequiresActionWebhook(String paymentIntentId) {
        paymentRepository.findByProcessorPaymentId(paymentIntentId).ifPresentOrElse(payment -> {
            PaymentStatus status = payment.getStatus();
            if (status == PaymentStatus.CAPTURED
                || status == PaymentStatus.REFUNDED
                || status == PaymentStatus.PARTIALLY_REFUNDED
                || status == PaymentStatus.FAILED) {
                log.info("Webhook payment_intent.requires_action ignored for finalized payment: paymentId={}, status={}",
                    payment.getId(), status);
                return;
            }

            recordEvent(payment, "PAYMENT_AUTHENTICATION_REQUIRED", status.name(), status.name());
            log.info("Webhook payment_intent.requires_action recorded: paymentId={}", payment.getId());
        }, () -> log.warn("Webhook payment_intent.requires_action ignored: no local payment for intentId={}", paymentIntentId));
    }

    public void handleStripePaymentIntentRequiresCaptureWebhook(String paymentIntentId) {
        paymentRepository.findByProcessorPaymentId(paymentIntentId).ifPresentOrElse(payment -> {
            PaymentStatus previousStatus = payment.getStatus();
            if (previousStatus == PaymentStatus.CAPTURED
                || previousStatus == PaymentStatus.REFUNDED
                || previousStatus == PaymentStatus.PARTIALLY_REFUNDED) {
                log.info("Webhook payment_intent.requires_capture ignored (already finalized): paymentId={}, status={}",
                    payment.getId(), previousStatus);
                return;
            }

            if (previousStatus == PaymentStatus.PENDING) {
                payment.setProcessor("stripe");
                payment.setFailureReason(null);
                payment.setFailureCode(null);
                payment.authorize();
                paymentRepository.save(payment);
                recordEvent(payment, "PAYMENT_AUTHORIZED", PaymentStatus.PENDING.name(), PaymentStatus.AUTHORIZED.name());
                safePublishPaymentEvent("PAYMENT_AUTHORIZED", payment, PaymentStatus.PENDING.name());
                previousStatus = PaymentStatus.AUTHORIZED;
            }

            if (Boolean.TRUE.equals(getMetadataBoolean(payment, "auto_capture_requested"))
                && payment.getStatus() == PaymentStatus.AUTHORIZED) {
                try {
                    capturePayment(payment.getId(), null);
                } catch (Exception ex) {
                    log.error("Webhook payment_intent.requires_capture auto-capture failed: paymentId={}", payment.getId(), ex);
                }
            }
        }, () -> log.warn("Webhook payment_intent.requires_capture ignored: no local payment for intentId={}", paymentIntentId));
    }

    public void handleStripePaymentIntentCanceledWebhook(String paymentIntentId) {
        paymentRepository.findByProcessorPaymentId(paymentIntentId).ifPresentOrElse(payment -> {
            PaymentStatus previousStatus = payment.getStatus();
            if (previousStatus == PaymentStatus.CAPTURED
                || previousStatus == PaymentStatus.REFUNDED
                || previousStatus == PaymentStatus.PARTIALLY_REFUNDED) {
                log.info("Webhook payment_intent.canceled ignored for finalized payment: paymentId={}, status={}",
                    payment.getId(), previousStatus);
                return;
            }
            if (previousStatus == PaymentStatus.FAILED || previousStatus == PaymentStatus.VOID) {
                log.info("Webhook payment_intent.canceled duplicate/no-op: paymentId={}, status={}",
                    payment.getId(), previousStatus);
                return;
            }

            payment.markFailed("Stripe payment intent canceled", "canceled");
            paymentRepository.save(payment);
            recordEvent(payment, "PAYMENT_FAILED", previousStatus.name(), PaymentStatus.FAILED.name());
        }, () -> log.warn("Webhook payment_intent.canceled ignored: no local payment for intentId={}", paymentIntentId));
    }

    public void handleStripeChargeRefundedWebhook(String paymentIntentId, Long amountRefundedInCents, String currency) {
        if (isBlank(paymentIntentId)) {
            log.warn("Webhook charge.refunded ignored: missing payment_intent");
            return;
        }

        paymentRepository.findByProcessorPaymentId(paymentIntentId).ifPresentOrElse(payment -> {
            PaymentStatus previousStatus = payment.getStatus();
            if (amountRefundedInCents == null || isBlank(currency)) {
                log.warn("Webhook charge.refunded missing amount/currency for paymentId={}", payment.getId());
                return;
            }

            long paymentAmount = payment.getAmount().getAmountInCents();
            if (amountRefundedInCents >= paymentAmount) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else if (amountRefundedInCents > 0) {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            } else {
                return;
            }

            paymentRepository.save(payment);
            if (previousStatus != payment.getStatus()) {
                recordEvent(payment, "PAYMENT_REFUNDED", previousStatus.name(), payment.getStatus().name());
            }
        }, () -> log.warn("Webhook charge.refunded ignored: no local payment for intentId={}", paymentIntentId));
    }

    public void handleStripeRefundWebhook(String paymentIntentId, String refundStatus, Long refundAmountInCents, String currency) {
        if (isBlank(paymentIntentId)) {
            log.warn("Webhook refund.* ignored: missing payment_intent");
            return;
        }

        String normalizedStatus = refundStatus == null ? "" : refundStatus.trim().toLowerCase(Locale.ROOT);
        if (!"succeeded".equals(normalizedStatus)) {
            log.info("Webhook refund.* ignored (status={}): paymentIntentId={}", refundStatus, paymentIntentId);
            return;
        }

        paymentRepository.findByProcessorPaymentId(paymentIntentId).ifPresentOrElse(payment -> {
            PaymentStatus previousStatus = payment.getStatus();
            if (previousStatus == PaymentStatus.REFUNDED || previousStatus == PaymentStatus.PARTIALLY_REFUNDED) {
                log.info("Webhook refund.* duplicate/no-op: paymentId={}, status={}", payment.getId(), previousStatus);
                return;
            }
            if (previousStatus != PaymentStatus.CAPTURED) {
                log.info("Webhook refund.* ignored for non-captured payment: paymentId={}, status={}",
                    payment.getId(), previousStatus);
                return;
            }

            if (refundAmountInCents != null && refundAmountInCents >= payment.getAmount().getAmountInCents()) {
                payment.setStatus(PaymentStatus.REFUNDED);
            } else {
                payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }

            paymentRepository.save(payment);
            recordEvent(payment, "PAYMENT_REFUNDED", previousStatus.name(), payment.getStatus().name());
        }, () -> log.warn("Webhook refund.* ignored: no local payment for intentId={}", paymentIntentId));
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

        if ("card".equalsIgnoreCase(request.getPaymentMethod().getType())
            && isBlank(request.getPaymentMethod().getSavedPaymentMethodId())) {
            throw new ValidationException("Stripe card payment requires a payment method ID (savedPaymentMethodId)");
        }

        if (request.getCustomer() == null || request.getCustomer().getName() == null || request.getCustomer().getName().isBlank()) {
            throw new ValidationException("Customer name is required for Stripe payments");
        }

        if (request.getCustomer().getAddress() == null
            || isBlank(request.getCustomer().getAddress().getLine1())
            || isBlank(request.getCustomer().getAddress().getCity())
            || isBlank(request.getCustomer().getAddress().getState())
            || isBlank(request.getCustomer().getAddress().getPostalCode())
            || isBlank(request.getCustomer().getAddress().getCountry())) {
            throw new ValidationException(
                "Customer address (line1, city, state, postalCode, country) is required for Stripe payments (India export compliance)"
            );
        }
    }

    private Boolean getMetadataBoolean(Payment payment, String key) {
        if (payment.getMetadata() == null) {
            return false;
        }
        Object value = payment.getMetadata().get(key);
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        return false;
    }

    private void enrichStripeCustomerMetadata(Map<String, Object> metadata, CustomerRequest customer) {
        if (customer == null) {
            return;
        }

        putIfHasText(metadata, "stripe_customer_email", customer.getEmail());
        putIfHasText(metadata, "stripe_customer_name", customer.getName());
        putIfHasText(metadata, "stripe_customer_phone", customer.getPhone());

        AddressRequest address = customer.getAddress();
        if (address == null) {
            return;
        }

        putIfHasText(metadata, "stripe_customer_address_line1", address.getLine1());
        putIfHasText(metadata, "stripe_customer_address_line2", address.getLine2());
        putIfHasText(metadata, "stripe_customer_address_city", address.getCity());
        putIfHasText(metadata, "stripe_customer_address_state", address.getState());
        putIfHasText(metadata, "stripe_customer_address_postal_code", address.getPostalCode());
        putIfHasText(metadata, "stripe_customer_address_country", address.getCountry());
    }

    private void enrichStripePaymentMethodMetadata(Map<String, Object> metadata, PaymentMethodRequest paymentMethod) {
        if (paymentMethod == null) {
            return;
        }

        putIfHasText(metadata, "stripe_payment_method_id", paymentMethod.getSavedPaymentMethodId());
        putIfHasText(metadata, "stripe_card_token", paymentMethod.getCardToken());
    }

    private void putIfHasText(Map<String, Object> metadata, String key, String value) {
        if (value != null && !value.isBlank()) {
            metadata.put(key, value);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void safePublishPaymentEvent(String eventType, Payment payment, String previousStatus) {
        try {
            eventPublisher.publishPaymentEvent(eventType, payment, previousStatus);
        } catch (Exception ex) {
            log.error("Failed to publish event (ignored): type={}, paymentId={}",
                eventType, payment.getId(), ex);
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
