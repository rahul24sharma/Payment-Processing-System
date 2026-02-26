package com.payment.fraud.controller;

import com.payment.fraud.ml.FeatureExtractor;
import com.payment.fraud.ml.FraudMLModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ml")
@Tag(name = "Machine Learning", description = "ML model information and testing")
@RequiredArgsConstructor
@Slf4j
public class MLController {
    
    private final FraudMLModel mlModel;
    private final FeatureExtractor featureExtractor;
    
    @Operation(summary = "Get ML model information")
    @GetMapping("/model-info")
    public ResponseEntity<Map<String, Object>> getModelInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("model_version", mlModel.getModelVersion());
        info.put("feature_count", featureExtractor.getFeatureNames().length);
        info.put("feature_names", featureExtractor.getFeatureNames());
        info.put("feature_importance", mlModel.getFeatureImportance(featureExtractor.getFeatureNames()));
        
        return ResponseEntity.ok(info);
    }
    
    @Operation(summary = "Test ML prediction with custom features")
    @PostMapping("/predict")
    public ResponseEntity<Map<String, Object>> testPrediction(
            @RequestBody Map<String, Object> request) {
        
        // Extract amount for testing
        Long amount = ((Number) request.get("amount")).longValue();
        
        // Create mock request for feature extraction
        com.payment.fraud.dto.FraudAssessmentRequest mockRequest = 
            com.payment.fraud.dto.FraudAssessmentRequest.builder()
                .paymentId(java.util.UUID.randomUUID())
                .merchantId(java.util.UUID.randomUUID())
                .amount(amount)
                .currency((String) request.getOrDefault("currency", "USD"))
                .metadata(new HashMap<>())
                .build();
        
        // Extract features
        double[] features = featureExtractor.extractFeatures(mockRequest);
        
        // Predict
        BigDecimal score = mlModel.predict(features);
        
        Map<String, Object> response = new HashMap<>();
        response.put("fraud_score", score);
        response.put("risk_level", determineRiskLevel(score));
        response.put("features", features);
        response.put("feature_names", featureExtractor.getFeatureNames());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ML Model Service is UP");
    }
    
    private String determineRiskLevel(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(75)) >= 0) return "critical";
        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) return "high";
        if (score.compareTo(BigDecimal.valueOf(40)) >= 0) return "medium";
        if (score.compareTo(BigDecimal.valueOf(20)) >= 0) return "low";
        return "very_low";
    }
}