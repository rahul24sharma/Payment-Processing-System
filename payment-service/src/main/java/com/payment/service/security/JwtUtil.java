package com.payment.service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@Slf4j
public class JwtUtil {
    
    @Value("${jwt.secret:my-super-secret-key-for-jwt-token-generation-minimum-256-bits-long}")
    private String jwtSecret;

    private SecretKey signingKey;

    @PostConstruct
    void initializeSigningKey() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET is missing. Set a value with at least 32 characters (>=256 bits)."
            );
        }

        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET is too short. Current length=" + keyBytes.length
                    + " bytes; minimum required is 32 bytes."
            );
        }

        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }
    
    public UUID extractMerchantId(String token) {
        String merchantIdStr = extractClaim(token, claims -> 
            claims.get("merchantId", String.class));
        return UUID.fromString(merchantIdStr);
    }
    
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
    
    private <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claimsResolver.apply(claims);
    }
    
    private SecretKey getSigningKey() {
        return signingKey;
    }
}
