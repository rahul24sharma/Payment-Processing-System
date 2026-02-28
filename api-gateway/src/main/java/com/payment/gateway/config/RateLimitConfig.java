package com.payment.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
public class RateLimitConfig {

    @Value("${jwt.secret:my-super-secret-key-for-jwt-token-generation-minimum-256-bits}")
    private String jwtSecret;

    @Bean
    public KeyResolver merchantOrIpKeyResolver() {
        return exchange -> {
            String merchantIdFromJwt = extractMerchantIdFromAuthorization(
                exchange.getRequest().getHeaders().getFirst("Authorization")
            );
            if (StringUtils.hasText(merchantIdFromJwt)) {
                return Mono.just("merchant:" + merchantIdFromJwt);
            }

            String merchantId = exchange.getRequest().getHeaders().getFirst("X-Merchant-Id");
            if (StringUtils.hasText(merchantId)) {
                return Mono.just("merchant:" + merchantId.trim());
            }

            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (StringUtils.hasText(forwardedFor)) {
                String ip = forwardedFor.split(",")[0].trim();
                if (StringUtils.hasText(ip)) {
                    return Mono.just("ip:" + ip);
                }
            }

            if (exchange.getRequest().getRemoteAddress() != null
                    && exchange.getRequest().getRemoteAddress().getAddress() != null) {
                return Mono.just(
                        "ip:" + exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                );
            }

            return Mono.just("ip:unknown");
        };
    }

    private String extractMerchantIdFromAuthorization(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authorizationHeader.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            return null;
        }

        try {
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

            String merchantId = claims.get("merchantId", String.class);
            if (StringUtils.hasText(merchantId)) {
                return merchantId.trim();
            }

            // Optional fallback if some clients encode merchant in subject.
            String subject = claims.getSubject();
            return StringUtils.hasText(subject) ? subject.trim() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
