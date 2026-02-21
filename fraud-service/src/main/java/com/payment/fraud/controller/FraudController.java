package com.payment.fraud.controller;

import com.payment.fraud.dto.FraudAssessmentRequest;
import com.payment.fraud.dto.FraudAssessmentResponse;
import com.payment.fraud.service.FraudAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fraud")
@Tag(name = "Fraud Detection", description = "Fraud assessment endpoints")
@RequiredArgsConstructor
@Slf4j
public class FraudController {
    
    private final FraudAssessmentService fraudAssessmentService;
    
    @Operation(
        summary = "Assess fraud risk",
        description = "Evaluates fraud risk for a payment and returns a risk score"
    )
    @PostMapping("/assess")
    public ResponseEntity<FraudAssessmentResponse> assessFraud(
            @Valid @RequestBody FraudAssessmentRequest request) {
        
        log.info("Received fraud assessment request: paymentId={}", request.getPaymentId());
        
        FraudAssessmentResponse response = fraudAssessmentService.assess(request);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Fraud Service is UP");
    }
}