package com.payment.service.client;

import com.payment.service.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class FraudServiceClient {
    
    private final RestTemplate restTemplate;
    
    /**
     * Call Fraud Service to assess risk
     */
    public BigDecimal assessRisk(Payment payment) {
        String url = "http://fraud-service/api/v1/fraud/assess";
        
        // Build request
        Map<String, Object> request = new HashMap<>();
        request.put("paymentId", payment.getId());
        request.put("merchantId", payment.getMerchantId());
        request.put("customerId", payment.getCustomerId());
        request.put("amount", payment.getAmount().getAmountInCents());
        request.put("currency", payment.getAmount().getCurrency());
        request.put("paymentMethodId", payment.getPaymentMethodId());
        
        try {
            // Call Fraud Service
            log.info("Calling fraud service: paymentId={}", payment.getId());
            
            Map<String, Object> response = restTemplate.postForObject(
                url, 
                request, 
                Map.class
            );
            
            if (response != null && response.containsKey("score")) {
                BigDecimal score = new BigDecimal(response.get("score").toString());
                log.info("Fraud assessment received: paymentId={}, score={}", 
                    payment.getId(), score);
                return score;
            }
            
        } catch (Exception e) {
            log.error("Fraud service call failed, using fallback score", e);
            // Fallback: return conservative default score
            return BigDecimal.valueOf(25);
        }
        
        return BigDecimal.ZERO;
    }
}