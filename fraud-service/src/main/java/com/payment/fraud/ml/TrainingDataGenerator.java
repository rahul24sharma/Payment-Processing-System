package com.payment.fraud.ml;

import com.payment.fraud.entity.FraudScore;
import com.payment.fraud.repository.FraudScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Generate training data for ML model
 * Exports fraud scores to CSV for model training
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TrainingDataGenerator {
    
    private final FraudScoreRepository fraudScoreRepository;
    
    /**
     * Export training data to CSV
     * Use this data to train your ML model in Python
     */
    public void exportTrainingData(String outputPath) throws IOException {
        log.info("Exporting training data to: {}", outputPath);
        
        // Get all fraud scores from last 90 days
        Instant since = Instant.now().minus(90, ChronoUnit.DAYS);
        List<FraudScore> scores = fraudScoreRepository.findHighScoresSince(since);
        
        try (FileWriter writer = new FileWriter(outputPath)) {
            // Write header
            writer.write("payment_id,velocity_score,rule_score,ml_score,final_score,risk_level,decision,is_fraud\n");
            
            // Write data
            for (FraudScore score : scores) {
                // Label: Consider BLOCK decisions or score > 75 as fraud
                int isFraud = ("BLOCK".equals(score.getDecision()) || 
                              score.getScore().compareTo(java.math.BigDecimal.valueOf(75)) > 0) 
                    ? 1 : 0;
                
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%d\n",
                    score.getPaymentId(),
                    score.getVelocityScore(),
                    score.getRuleScore(),
                    score.getMlScore(),
                    score.getScore(),
                    score.getRiskLevel(),
                    score.getDecision(),
                    isFraud
                ));
            }
        }
        
        log.info("Exported {} records to {}", scores.size(), outputPath);
    }
}