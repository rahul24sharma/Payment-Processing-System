package com.payment.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/payment")
    public ResponseEntity<Map<String, String>> paymentFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "service", "payment-service",
                        "message", "Payment service is currently unavailable. Please try again later."
                ));
    }

    @RequestMapping("/fraud")
    public ResponseEntity<Map<String, String>> fraudFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "service", "fraud-service",
                        "message", "Fraud detection service is currently unavailable. Please try again later."
                ));
    }

    @RequestMapping("/merchant")
    public ResponseEntity<Map<String, String>> merchantFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "service", "merchant-service",
                        "message", "Merchant service is currently unavailable. Please try again later."
                ));
    }
}
