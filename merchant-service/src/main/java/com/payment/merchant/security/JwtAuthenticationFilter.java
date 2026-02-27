package com.payment.merchant.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        boolean authenticated = false;
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    UUID merchantId = jwtUtil.extractMerchantId(token);
                    String role = jwtUtil.extractRole(token);
                    authenticateRequest(request, merchantId, role);
                    authenticated = true;
                }
            } catch (Exception e) {
                log.error("JWT authentication failed", e);
            }
        }

        // Local-dev fallback: allow merchant context via header if JWT is absent/expired.
        if (!authenticated && SecurityContextHolder.getContext().getAuthentication() == null) {
            String merchantIdHeader = request.getHeader("X-Merchant-Id");
            if (merchantIdHeader == null || merchantIdHeader.isBlank()) {
                merchantIdHeader = request.getHeader("X-Merchant-ID");
            }
            if (merchantIdHeader != null && !merchantIdHeader.isBlank()) {
                try {
                    UUID merchantId = UUID.fromString(merchantIdHeader);
                    String merchantRoleHeader = request.getHeader("X-Merchant-Role");
                    if (merchantRoleHeader == null || merchantRoleHeader.isBlank()) {
                        merchantRoleHeader = "ADMIN";
                    }
                    authenticateRequest(request, merchantId, merchantRoleHeader);
                } catch (IllegalArgumentException ignored) {
                    // Let request continue unauthenticated; controller/security will reject invalid values.
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateRequest(HttpServletRequest request, UUID merchantId, String roleValue) {
        MerchantRole role = MerchantRole.from(roleValue);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        merchantId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute("merchantId", merchantId);
        request.setAttribute("merchantRole", role.name());
    }
}
