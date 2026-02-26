package com.payment.fraud.ml;

import com.payment.fraud.dto.FraudAssessmentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * Extract features from payment for ML model
 */
@Component
@Slf4j
public class FeatureExtractor {
    
    /**
     * Extract features for fraud prediction
     * Returns normalized feature vector
     */
    public double[] extractFeatures(FraudAssessmentRequest request) {
        // Feature engineering
        Map<String, Double> features = new HashMap<>();
        
        // Feature 1: Log amount (to handle wide range)
        double logAmount = Math.log(request.getAmount() + 1);
        features.put("log_amount", logAmount);
        
        // Feature 2: Amount in original scale (normalized)
        double normalizedAmount = request.getAmount() / 100000.0; // Normalize by $1000
        features.put("normalized_amount", normalizedAmount);
        
        // Feature 3: Is round amount? (suspicious pattern)
        double isRoundAmount = (request.getAmount() % 10000 == 0) ? 1.0 : 0.0;
        features.put("is_round_amount", isRoundAmount);
        
        // Feature 4: Is very round? (even more suspicious)
        double isVeryRound = (request.getAmount() % 100000 == 0) ? 1.0 : 0.0;
        features.put("is_very_round", isVeryRound);
        
        // Feature 5: Hour of day (fraud peaks at certain hours)
        Instant now = Instant.now();
        int hourOfDay = now.atZone(ZoneOffset.UTC).getHour();
        double normalizedHour = hourOfDay / 24.0;
        features.put("hour_of_day", normalizedHour);
        
        // Feature 6: Day of week (0-6)
        int dayOfWeek = now.atZone(ZoneOffset.UTC).getDayOfWeek().getValue();
        double normalizedDay = dayOfWeek / 7.0;
        features.put("day_of_week", normalizedDay);
        
        // Feature 7: Is weekend?
        double isWeekend = (dayOfWeek >= 6) ? 1.0 : 0.0;
        features.put("is_weekend", isWeekend);
        
        // Feature 8: Is late night? (00:00 - 06:00)
        double isLateNight = (hourOfDay >= 0 && hourOfDay < 6) ? 1.0 : 0.0;
        features.put("is_late_night", isLateNight);
        
        // Feature 9: Currency encoding (USD=0, EUR=1, GBP=2, etc.)
        double currencyCode = switch (request.getCurrency().toUpperCase()) {
            case "USD" -> 0.0;
            case "EUR" -> 1.0;
            case "GBP" -> 2.0;
            default -> 3.0;
        };
        features.put("currency_code", currencyCode / 3.0); // Normalize
        
        // Feature 10: Has metadata? (legitimate payments usually have order info)
        double hasMetadata = (request.getMetadata() != null && !request.getMetadata().isEmpty()) 
            ? 1.0 : 0.0;
        features.put("has_metadata", hasMetadata);
        
        log.debug("Extracted {} features for payment: {}", features.size(), request.getPaymentId());
        
        // Convert to array (order matters!)
        return new double[] {
            features.get("log_amount"),
            features.get("normalized_amount"),
            features.get("is_round_amount"),
            features.get("is_very_round"),
            features.get("hour_of_day"),
            features.get("day_of_week"),
            features.get("is_weekend"),
            features.get("is_late_night"),
            features.get("currency_code"),
            features.get("has_metadata")
        };
    }
    
    /**
     * Get feature names (for debugging)
     */
    public String[] getFeatureNames() {
        return new String[] {
            "log_amount",
            "normalized_amount",
            "is_round_amount",
            "is_very_round",
            "hour_of_day",
            "day_of_week",
            "is_weekend",
            "is_late_night",
            "currency_code",
            "has_metadata"
        };
    }
}