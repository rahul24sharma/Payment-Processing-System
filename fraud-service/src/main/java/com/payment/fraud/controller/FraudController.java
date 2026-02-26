package com.payment.fraud.controller;

import com.payment.fraud.dto.FraudAssessmentRequest;
import com.payment.fraud.dto.FraudAssessmentResponse;
import com.payment.fraud.dto.CreateFraudRuleRequest;
import com.payment.fraud.dto.FraudRuleResponse;
import com.payment.fraud.dto.UpdateFraudRuleRequest;
import com.payment.fraud.service.FraudAssessmentService;
import com.payment.fraud.service.FraudRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud")
@Tag(name = "Fraud Detection", description = "Fraud assessment endpoints")
@RequiredArgsConstructor
@Slf4j
public class FraudController {
    
    private final FraudAssessmentService fraudAssessmentService;
    private final FraudRuleService fraudRuleService;
    
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

    @Operation(summary = "List fraud rules")
    @GetMapping("/rules")
    public ResponseEntity<List<FraudRuleResponse>> listRules() {
        return ResponseEntity.ok(fraudRuleService.listRules());
    }

    @Operation(summary = "Create fraud rule")
    @PostMapping("/rules")
    public ResponseEntity<FraudRuleResponse> createRule(
            @Valid @RequestBody CreateFraudRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fraudRuleService.createRule(request));
    }

    @Operation(summary = "Update fraud rule")
    @PutMapping("/rules/{id}")
    public ResponseEntity<FraudRuleResponse> updateRule(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFraudRuleRequest request) {
        return ResponseEntity.ok(fraudRuleService.updateRule(id, request));
    }

    @Operation(summary = "Delete fraud rule")
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        fraudRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Fraud Service is UP");
    }
}
