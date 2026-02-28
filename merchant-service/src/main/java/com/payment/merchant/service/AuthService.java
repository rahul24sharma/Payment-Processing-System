package com.payment.merchant.service;

import com.payment.merchant.dto.AuthResponse;
import com.payment.merchant.dto.ForgotPasswordResponse;
import com.payment.merchant.dto.LoginRequest;
import com.payment.merchant.dto.RegisterRequest;
import com.payment.merchant.dto.ResetPasswordResponse;
import com.payment.merchant.entity.Merchant;
import com.payment.merchant.repository.MerchantRepository;
import com.payment.merchant.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
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
    @Value("${merchant.auth.password-reset.token-validity-minutes:30}")
    private long passwordResetTokenValidityMinutes;
    @Value("${merchant.auth.password-reset.expose-token-in-response:true}")
    private boolean exposeResetTokenInResponse;
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
            .role("ADMIN")
            .build();
        
        merchant = merchantRepository.save(merchant);
        
        // Generate JWT token
        String token = jwtUtil.generateToken(merchant.getId(), merchant.getEmail(), merchant.getRole());
        
        log.info("Merchant registered successfully: id={}", merchant.getId());
        
        return AuthResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .merchantId(merchant.getId().toString())
            .businessName(merchant.getBusinessName())
            .email(merchant.getEmail())
            .role(merchant.getRole())
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
        String token = jwtUtil.generateToken(merchant.getId(), merchant.getEmail(), merchant.getRole());
        
        log.info("Merchant logged in successfully: id={}", merchant.getId());
        
        return AuthResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .merchantId(merchant.getId().toString())
            .businessName(merchant.getBusinessName())
            .email(merchant.getEmail())
            .role(merchant.getRole())
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
     * Request password reset for merchant account.
     * Always returns success message to avoid account enumeration.
     */
    public ForgotPasswordResponse requestPasswordReset(String email) {
        log.info("Password reset requested: email={}", email);
        Merchant merchant = merchantRepository.findByEmail(email).orElse(null);

        String issuedToken = null;
        if (merchant != null) {
            issuedToken = generateResetToken();
            merchant.setPasswordResetTokenHash(sha256Hex(issuedToken));
            merchant.setPasswordResetRequestedAt(Instant.now());
            merchant.setPasswordResetTokenExpiresAt(
                Instant.now().plus(Duration.ofMinutes(passwordResetTokenValidityMinutes))
            );
            merchantRepository.save(merchant);
            log.info("Password reset token generated: merchantId={}", merchant.getId());
        }

        return ForgotPasswordResponse.builder()
            .message("If an account exists for this email, password reset instructions have been sent.")
            .resetToken(exposeResetTokenInResponse ? issuedToken : null)
            .build();
    }

    /**
     * Reset password with valid reset token.
     */
    public ResetPasswordResponse resetPassword(String token, String newPassword) {
        Merchant merchant = merchantRepository
            .findByPasswordResetTokenHashAndPasswordResetTokenExpiresAtAfter(
                sha256Hex(token),
                Instant.now()
            )
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        merchant.setPasswordHash(passwordEncoder.encode(newPassword));
        merchant.setPasswordResetTokenHash(null);
        merchant.setPasswordResetRequestedAt(null);
        merchant.setPasswordResetTokenExpiresAt(null);
        merchantRepository.save(merchant);

        log.info("Password reset completed: merchantId={}", merchant.getId());
        return ResetPasswordResponse.builder()
            .message("Password reset successful. You can now log in.")
            .build();
    }
    
    /**
     * Generate secure API key
     */
    private String generateApiKey() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String generateResetToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
