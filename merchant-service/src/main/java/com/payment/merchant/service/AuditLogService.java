package com.payment.merchant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.merchant.entity.AuditLog;
import com.payment.merchant.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public void log(
        UUID merchantId,
        String actorType,
        String actorId,
        String actorRole,
        String action,
        String targetType,
        String targetId,
        String outcome,
        Map<String, Object> details,
        HttpServletRequest request
    ) {
        try {
            AuditLog auditLog = AuditLog.builder()
                .merchantId(merchantId)
                .actorType(actorType)
                .actorId(actorId)
                .actorRole(actorRole)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .outcome(outcome)
                .detailsJson(details == null ? null : toJson(details))
                .ipAddress(request != null ? request.getRemoteAddr() : null)
                .userAgent(request != null ? truncate(request.getHeader("User-Agent"), 2000) : null)
                .createdAt(Instant.now())
                .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to persist audit log: action={}, targetType={}, targetId={}",
                action, targetType, targetId, e);
        }
    }

    private String toJson(Map<String, Object> details) throws JsonProcessingException {
        return objectMapper.writeValueAsString(details);
    }

    private String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}
