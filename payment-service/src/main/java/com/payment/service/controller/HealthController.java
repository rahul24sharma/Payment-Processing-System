package com.payment.service.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Slf4j
@Hidden // Hide from Swagger docs (we use Actuator for this)
public class HealthController {
    
    private final DataSource dataSource;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "payment-service");
        health.put("timestamp", System.currentTimeMillis());
        
        // Check database
        try (Connection conn = dataSource.getConnection()) {
            health.put("database", conn.isValid(2) ? "UP" : "DOWN");
        } catch (Exception e) {
            health.put("database", "DOWN");
            health.put("database_error", e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }
}