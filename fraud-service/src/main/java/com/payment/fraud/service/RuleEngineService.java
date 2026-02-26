package com.payment.fraud.service;

import com.payment.fraud.dto.FraudAssessmentRequest;
import com.payment.fraud.entity.FraudRule;
import com.payment.fraud.repository.FraudRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class RuleEngineService {
    
    private final FraudRuleRepository fraudRuleRepository;
    
    /**
     * Evaluate all active fraud rules
     */
    public BigDecimal evaluateRules(FraudAssessmentRequest request) {
        log.debug("Evaluating fraud rules: paymentId={}", request.getPaymentId());
        
        List<FraudRule> activeRules = fraudRuleRepository.findActiveRulesByPriority();
        
        BigDecimal totalScore = BigDecimal.ZERO;
        List<String> triggeredRules = new ArrayList<>();
        
        for (FraudRule rule : activeRules) {
            boolean triggered = evaluateRule(rule, request);
            
            if (triggered) {
                triggeredRules.add(rule.getRuleName());
                
                if (rule.getScoreImpact() != null) {
                    totalScore = totalScore.add(BigDecimal.valueOf(rule.getScoreImpact()));
                }
                
                // If action is BLOCK, return max score immediately
                if ("BLOCK".equals(rule.getAction())) {
                    log.warn("BLOCK rule triggered: rule={}, paymentId={}", 
                        rule.getRuleName(), request.getPaymentId());
                    return BigDecimal.valueOf(100);
                }
            }
        }
        
        log.debug("Rule evaluation completed: score={}, triggeredRules={}", 
            totalScore, triggeredRules);
        
        return totalScore.min(BigDecimal.valueOf(100));
    }
    
    /**
     * Evaluate a single rule
     */
    private boolean evaluateRule(FraudRule rule, FraudAssessmentRequest request) {
        Map<String, Object> conditions = rule.getConditions();
        
        return switch (rule.getRuleType()) {
            case "AMOUNT" -> evaluateAmountRule(conditions, request);
            case "VELOCITY" -> false; // Handled by VelocityCheckService
            case "GEOLOCATION" -> evaluateGeolocationRule(conditions, request);
            case "PATTERN" -> evaluatePatternRule(conditions, request);
            default -> {
                log.warn("Unknown rule type: {}", rule.getRuleType());
                yield false;
            }
        };
    }
    
    /**
     * Evaluate amount-based rule
     */
    private boolean evaluateAmountRule(Map<String, Object> conditions, 
                                      FraudAssessmentRequest request) {
        Object threshold = conditions.get("threshold");
        if (threshold instanceof Number) {
            long thresholdAmount = ((Number) threshold).longValue();
            return request.getAmount() > thresholdAmount;
        }
        return false;
    }
    
    /**
     * Evaluate geolocation rule (MVP stub)
     */
    private boolean evaluateGeolocationRule(Map<String, Object> conditions, 
                                           FraudAssessmentRequest request) {
        // MVP implementation:
        // Read country code from request metadata (e.g. "country", "countryCode", "ipCountry")
        // and compare against allow/block lists in the rule conditions.
        String country = extractCountryCode(request);
        if (country == null) {
            return false;
        }

        Set<String> blockedCountries = normalizeCountrySet(conditions.get("blockedCountries"));
        if (!blockedCountries.isEmpty() && blockedCountries.contains(country)) {
            return true;
        }

        Set<String> allowedCountries = normalizeCountrySet(conditions.get("allowedCountries"));
        if (!allowedCountries.isEmpty()) {
            return !allowedCountries.contains(country);
        }

        Object exactCountry = conditions.get("country");
        if (exactCountry instanceof String s && !s.isBlank()) {
            return country.equalsIgnoreCase(s.trim());
        }

        return false;
    }
    
    /**
     * Evaluate pattern rule (MVP stub)
     */
    private boolean evaluatePatternRule(Map<String, Object> conditions, 
                                       FraudAssessmentRequest request) {
        int matchedConditions = 0;

        // 1) Round amount pattern (e.g. exact $100.00 -> cents value divisible by 10000)
        Boolean roundAmount = asBoolean(conditions.get("roundAmount"));
        if (Boolean.TRUE.equals(roundAmount) && request.getAmount() % 10_000 == 0) {
            matchedConditions++;
        }

        // 2) Amount modulo check (more generic than roundAmount)
        Long amountModulo = asLong(conditions.get("amountModulo"));
        if (amountModulo != null && amountModulo > 0 && request.getAmount() % amountModulo == 0) {
            matchedConditions++;
        }

        // 3) User agent substring matching (metadata or request.userAgent)
        String userAgent = request.getUserAgent();
        Set<String> userAgentContains = normalizeStringSet(conditions.get("userAgentContains"));
        if (userAgent != null && !userAgentContains.isEmpty()) {
            String ua = userAgent.toLowerCase();
            boolean uaMatched = userAgentContains.stream().anyMatch(s -> ua.contains(s.toLowerCase()));
            if (uaMatched) {
                matchedConditions++;
            }
        }

        // 4) Suspicious email domains (expects metadata.email or customerEmail)
        String email = getMetadataString(request, "email", "customerEmail");
        Set<String> suspiciousDomains = normalizeStringSet(conditions.get("suspiciousEmailDomains"));
        if (email != null && email.contains("@") && !suspiciousDomains.isEmpty()) {
            String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
            boolean domainMatched = suspiciousDomains.stream()
                .map(String::toLowerCase)
                .anyMatch(domain::equals);
            if (domainMatched) {
                matchedConditions++;
            }
        }

        // 5) Reused device / card counters supplied by upstream enrichment in metadata
        Long minDeviceReuseCount = asLong(conditions.get("minDeviceReuseCount"));
        Long deviceReuseCount = getMetadataLong(request, "deviceReuseCount");
        if (minDeviceReuseCount != null && deviceReuseCount != null && deviceReuseCount >= minDeviceReuseCount) {
            matchedConditions++;
        }

        Long minCardReuseCount = asLong(conditions.get("minCardReuseCount"));
        Long cardReuseCount = getMetadataLong(request, "cardReuseCount");
        if (minCardReuseCount != null && cardReuseCount != null && cardReuseCount >= minCardReuseCount) {
            matchedConditions++;
        }

        // 6) New customer pattern
        Boolean newCustomerOnly = asBoolean(conditions.get("newCustomerOnly"));
        if (Boolean.TRUE.equals(newCustomerOnly)) {
            Long customerAgeDays = getMetadataLong(request, "customerAgeDays");
            if (customerAgeDays != null && customerAgeDays <= 7) {
                matchedConditions++;
            }
        }

        // If no recognized conditions were provided, do not trigger.
        int configuredConditions = countRecognizedPatternConditions(conditions);
        if (configuredConditions == 0) {
            return false;
        }

        // By default all configured conditions must match; allow threshold override.
        Long minMatches = asLong(conditions.get("minMatches"));
        int requiredMatches = minMatches != null && minMatches > 0
            ? Math.min(minMatches.intValue(), configuredConditions)
            : configuredConditions;

        return matchedConditions >= requiredMatches;
    }

    private String extractCountryCode(FraudAssessmentRequest request) {
        if (request.getMetadata() == null || request.getMetadata().isEmpty()) {
            return null;
        }

        for (String key : List.of("country", "countryCode", "ipCountry")) {
            Object value = request.getMetadata().get(key);
            if (value instanceof String s && !s.isBlank()) {
                return s.trim().toUpperCase();
            }
        }

        return null;
    }

    private Set<String> normalizeCountrySet(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return Set.of();
        }

        return collection.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .map(String::toUpperCase)
            .collect(Collectors.toSet());
    }

    private Set<String> normalizeStringSet(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return Set.of();
        }
        return collection.stream()
            .filter(Objects::nonNull)
            .map(Object::toString)
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toSet());
    }

    private String getMetadataString(FraudAssessmentRequest request, String... keys) {
        if (request.getMetadata() == null) {
            return null;
        }
        for (String key : keys) {
            Object value = request.getMetadata().get(key);
            if (value instanceof String s && !s.isBlank()) {
                return s.trim();
            }
        }
        return null;
    }

    private Long getMetadataLong(FraudAssessmentRequest request, String key) {
        if (request.getMetadata() == null) {
            return null;
        }
        return asLong(request.getMetadata().get(key));
    }

    private Long asLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Boolean asBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s && !s.isBlank()) {
            return Boolean.parseBoolean(s.trim());
        }
        return null;
    }

    private int countRecognizedPatternConditions(Map<String, Object> conditions) {
        int count = 0;
        for (String key : List.of(
            "roundAmount",
            "amountModulo",
            "userAgentContains",
            "suspiciousEmailDomains",
            "minDeviceReuseCount",
            "minCardReuseCount",
            "newCustomerOnly"
        )) {
            if (conditions.containsKey(key)) {
                count++;
            }
        }
        return count;
    }
}
