package com.payment.fraud.ml;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple ML model for fraud detection
 * 
 * For MVP: Using logistic regression with hand-tuned weights
 * In production: Load trained model from file (TensorFlow, PyTorch, ONNX)
 */
@Component
@Slf4j
public class FraudMLModel {
    
    // Model weights (learned from training data)
    // In production, these would be loaded from a saved model file
    private static final double[] WEIGHTS = {
        2.5,   // log_amount (high amounts = higher risk)
        3.0,   // normalized_amount
        1.5,   // is_round_amount
        2.0,   // is_very_round
        0.5,   // hour_of_day
        0.3,   // day_of_week
        1.2,   // is_weekend (slight increase)
        2.5,   // is_late_night (significant increase)
        0.2,   // currency_code
        -1.0   // has_metadata (negative = reduces risk)
    };
    
    private static final double BIAS = -5.0; // Model bias term
    
    private static final String MODEL_VERSION = "1.0.0-logistic-regression";
    
    /**
     * Predict fraud probability
     * Returns score from 0-100
     */
    public BigDecimal predict(double[] features) {
        if (features.length != WEIGHTS.length) {
            throw new IllegalArgumentException(
                String.format("Expected %d features, got %d", WEIGHTS.length, features.length)
            );
        }
        
        // Logistic regression: y = sigmoid(w·x + b)
        RealVector featureVector = new ArrayRealVector(features);
        RealVector weightVector = new ArrayRealVector(WEIGHTS);
        
        // Dot product
        double dotProduct = weightVector.dotProduct(featureVector);
        
        // Add bias
        double z = dotProduct + BIAS;
        
        // Sigmoid activation
        double probability = sigmoid(z);
        
        // Scale to 0-100
        double score = probability * 100;
        
        // Cap at 100
        score = Math.min(100, Math.max(0, score));
        
        log.debug("ML prediction: probability={}, score={}", probability, score);
        
        return BigDecimal.valueOf(score).setScale(2, java.math.RoundingMode.HALF_UP);
    }
    
    /**
     * Sigmoid function: 1 / (1 + e^(-x))
     */
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }
    
    /**
     * Get model version
     */
    public String getModelVersion() {
        return MODEL_VERSION;
    }
    
    /**
     * Get feature importance (which features matter most)
     */
    public Map<String, Double> getFeatureImportance(String[] featureNames) {
        Map<String, Double> importance = new HashMap<>();
        
        for (int i = 0; i < WEIGHTS.length && i < featureNames.length; i++) {
            importance.put(featureNames[i], Math.abs(WEIGHTS[i]));
        }
        
        return importance;
    }
}