package com.payment.merchant.service;

import com.payment.merchant.dto.AuthResponse;
import com.payment.merchant.dto.LoginRequest;
import com.payment.merchant.dto.RegisterRequest;
import com.payment.merchant.entity.Merchant;
import com.payment.merchant.repository.MerchantRepository;
import com.payment.merchant.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    
    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Register a new merchant
     */
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new merchant: email={}", request.getEmail());
        
        // Check if email already exists
        if (merchantRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        
        // Generate API key
        String apiKey = generateApiKey();
        String apiKeyHash = passwordEncoder.encode(apiKey);
        
        // Create merchant
        Merchant merchant = Merchant.builder()
            .businessName(request.getBusinessName())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .apiKeyHash(apiKeyHash)
            .apiKeyPrefix("sk_test_")
            .status("ACTIVE")
            .riskProfile("MEDIUM")
            .build();
        
        merchant = merchantRepository.save(merchant);
        
        // Generate JWT token
        String token = jwtUtil.generateToken(merchant.getId(), merchant.getEmail());
        
        log.info("Merchant registered successfully: id={}", merchant.getId());
        
        return AuthResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .merchantId(merchant.getId().toString())
            .businessName(merchant.getBusinessName())
            .email(merchant.getEmail())
            .apiKey("sk_test_" + apiKey) // Return once during registration
            .build();
    }
    
    /**
     * Login merchant
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Merchant login attempt: email={}", request.getEmail());
        
        Merchant merchant = merchantRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), merchant.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        
        // Check if merchant is active
        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }
        
        // Generate JWT token
        String token = jwtUtil.generateToken(merchant.getId(), merchant.getEmail());
        
        log.info("Merchant logged in successfully: id={}", merchant.getId());
        
        return AuthResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .merchantId(merchant.getId().toString())
            .businessName(merchant.getBusinessName())
            .email(merchant.getEmail())
            .build();
    }
    
    /**
     * Validate API key
     */
    public Merchant validateApiKey(String apiKey) {
        // Hash the provided API key
        String providedHash = passwordEncoder.encode(apiKey);
        
        // In production, you'd find by hash directly
        // For now, we'll iterate (not efficient, but works for MVP)
        return merchantRepository.findAll().stream()
            .filter(m -> passwordEncoder.matches(apiKey, m.getApiKeyHash()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));
    }
    
    /**
     * Generate secure API key
     */
    private String generateApiKey() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}