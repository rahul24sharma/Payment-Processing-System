package com.payment.service.controller;

import com.payment.service.dto.request.CapturePaymentRequest;
import com.payment.service.dto.request.CreatePaymentRequest;
import com.payment.service.dto.request.RefundRequest;
import com.payment.service.dto.response.PaymentListResponse;
import com.payment.service.dto.response.PaymentResponse;
import com.payment.service.dto.response.RefundResponse;
import com.payment.service.entity.Payment;
import com.payment.service.entity.PaymentStatus;
import com.payment.service.entity.Refund;
import com.payment.service.mapper.PaymentMapper;
import com.payment.service.service.PaymentOperationResult;
import com.payment.service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment processing endpoints")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;
    
    /**
     * Create a new payment
     */
    @Operation(
        summary = "Create a new payment",
        description = "Creates a payment intent. Can authorize immediately or authorize and capture."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "402", description = "Payment declined"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Parameter(description = "Payment creation request", required = true)
            @Valid @RequestBody CreatePaymentRequest request,
            
            @Parameter(
                description = "Idempotency key to prevent duplicate requests",
                required = true
            )
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestAttribute("merchantId") UUID merchantId) {
        
        log.info("Received payment creation request: amount={}, currency={}, idempotencyKey={}", 
            request.getAmount(), request.getCurrency(), idempotencyKey);
        
        PaymentOperationResult result = paymentService.createPayment(request, idempotencyKey, merchantId);
        Payment payment = result.getPayment();
        PaymentResponse response = paymentMapper.toResponse(payment, result.getNextAction());
        
        log.info("Payment created: id={}, status={}", payment.getId(), payment.getStatus());
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
    
    /**
     * Get payment by ID
     */
    @Operation(
        summary = "Retrieve a payment",
        description = "Retrieves the details of a payment that has been previously created."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable String id) {
        
        log.info("Retrieving payment: id={}", id);
        
        Payment payment = paymentService.getPayment(UUID.fromString(id));
        PaymentResponse response = paymentMapper.toResponse(payment);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Authorize a pending payment
     */
    @Operation(
        summary = "Authorize a payment",
        description = "Authorizes a previously created pending payment without capturing it."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment authorized successfully"),
        @ApiResponse(responseCode = "404", description = "Payment not found"),
        @ApiResponse(responseCode = "409", description = "Payment cannot be authorized in current state")
    })
    @PostMapping("/{id}/authorize")
    public ResponseEntity<PaymentResponse> authorizePayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable String id) {

        log.info("Authorizing payment: id={}", id);

        PaymentOperationResult result = paymentService.authorizePayment(UUID.fromString(id));
        PaymentResponse response = paymentMapper.toResponse(result.getPayment(), result.getNextAction());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete-authentication")
    public ResponseEntity<PaymentResponse> completeAuthentication(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable String id) {

        log.info("Completing payment authentication: id={}", id);
        PaymentOperationResult result = paymentService.completeAuthentication(UUID.fromString(id));
        PaymentResponse response = paymentMapper.toResponse(result.getPayment(), result.getNextAction());
        return ResponseEntity.ok(response);
    }
    
    /**
     * List payments
     */
    @Operation(
        summary = "List all payments",
        description = "Returns a list of payments. Results can be filtered by status."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<PaymentListResponse> listPayments(
            @Parameter(description = "Filter by payment status")
            @RequestParam(required = false) PaymentStatus status,

            @Parameter(description = "Number of results (max 100)")
            @RequestParam(defaultValue = "20")
            @Min(1) @Max(100) int limit,
            @RequestAttribute("merchantId") UUID merchantId) {

        log.info("Listing payments: status={}, limit={}", status, limit);
        
        List<Payment> payments = paymentService.listPayments(merchantId, status, limit);
        
        List<PaymentResponse> paymentResponses = payments.stream()
            .map(paymentMapper::toResponse)
            .collect(Collectors.toList());
        
        PaymentListResponse response = PaymentListResponse.builder()
            .data(paymentResponses)
            .hasMore(false) // For MVP, no pagination
            .totalCount((long) paymentResponses.size())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Capture an authorized payment
     */
    @Operation(
        summary = "Capture an authorized payment",
        description = "Captures a previously authorized payment. This charges the customer."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment captured successfully"),
        @ApiResponse(responseCode = "404", description = "Payment not found"),
        @ApiResponse(responseCode = "409", description = "Payment cannot be captured in current state")
    })
    @PostMapping("/{id}/capture")
    public ResponseEntity<PaymentResponse> capturePayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable String id,
            
            @Parameter(description = "Capture request (optional)")
            @RequestBody(required = false) CapturePaymentRequest request) {
        
        log.info("Capturing payment: id={}", id);
        
        Payment payment = paymentService.capturePayment(UUID.fromString(id), request);
        PaymentResponse response = paymentMapper.toResponse(payment);
        
        log.info("Payment captured: id={}", id);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Void an authorized payment
     */
    @Operation(
        summary = "Void an authorized payment",
        description = "Cancels an authorized payment before it has been captured."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment voided successfully"),
        @ApiResponse(responseCode = "404", description = "Payment not found"),
        @ApiResponse(responseCode = "409", description = "Payment cannot be voided in current state")
    })
    @PostMapping("/{id}/void")
    public ResponseEntity<PaymentResponse> voidPayment(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable String id) {
        
        log.info("Voiding payment: id={}", id);
        
        Payment payment = paymentService.voidPayment(UUID.fromString(id));
        PaymentResponse response = paymentMapper.toResponse(payment);
        
        log.info("Payment voided: id={}", id);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create a refund
     */
    @Operation(
        summary = "Refund a payment",
        description = "Creates a refund for a captured payment. Supports full and partial refunds."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Refund created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid refund amount"),
        @ApiResponse(responseCode = "404", description = "Payment not found"),
        @ApiResponse(responseCode = "409", description = "Payment cannot be refunded")
    })
    @PostMapping("/{id}/refunds")
    public ResponseEntity<RefundResponse> createRefund(
            @Parameter(description = "Payment ID", required = true)
            @PathVariable String id,
            
            @Parameter(description = "Refund request")
            @Valid @RequestBody RefundRequest request) {
        
        log.info("Creating refund: paymentId={}, amount={}", id, request.getAmount());
        
        Refund refund = paymentService.refundPayment(UUID.fromString(id), request);
        RefundResponse response = paymentMapper.toRefundResponse(refund);
        
        log.info("Refund created: refundId={}, paymentId={}", refund.getId(), id);
        
        return ResponseEntity.ok(response);
    }
}
