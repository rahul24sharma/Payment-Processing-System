package com.payment.settlement.client;

import com.payment.settlement.dto.PaymentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceClient {
    
    private final RestTemplate restTemplate;
    
    /**
     * Fetch captured payments for settlement
     * In production, this would be a dedicated endpoint
     * For MVP, we'll mock this
     */
    public List<PaymentDTO> getCapturedPayments(Instant start, Instant end) {
        // TODO: In production, call:
        // GET http://payment-service/api/v1/payments/captured?start={start}&end={end}
        
        log.info("Fetching captured payments between {} and {}", start, end);
        
        // For MVP, return mock data
        // In production, this would be a REST call to Payment Service
        return List.of();
    }
}