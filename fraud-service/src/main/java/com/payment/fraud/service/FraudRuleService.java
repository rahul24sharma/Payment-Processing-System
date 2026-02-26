package com.payment.fraud.service;

import com.payment.fraud.dto.CreateFraudRuleRequest;
import com.payment.fraud.dto.FraudRuleResponse;
import com.payment.fraud.dto.UpdateFraudRuleRequest;
import com.payment.fraud.entity.FraudRule;
import com.payment.fraud.exception.RuleNotFoundException;
import com.payment.fraud.repository.FraudRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudRuleService {

    private final FraudRuleRepository fraudRuleRepository;

    @Transactional(readOnly = true)
    public List<FraudRuleResponse> listRules() {
        return fraudRuleRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(
                        b.getPriority() != null ? b.getPriority() : 0,
                        a.getPriority() != null ? a.getPriority() : 0))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FraudRuleResponse createRule(CreateFraudRuleRequest request) {
        FraudRule rule = FraudRule.builder()
                .ruleName(request.getRuleName().trim())
                .ruleType(request.getRuleType().trim().toUpperCase())
                .conditions(request.getConditions())
                .action(request.getAction().trim().toUpperCase())
                .scoreImpact(request.getScoreImpact())
                .priority(request.getPriority() != null ? request.getPriority() : 100)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdBy(request.getCreatedBy())
                .build();

        FraudRule saved = fraudRuleRepository.save(rule);
        log.info("Created fraud rule: id={}, name={}, type={}", saved.getId(), saved.getRuleName(), saved.getRuleType());
        return toResponse(saved);
    }

    @Transactional
    public FraudRuleResponse updateRule(UUID ruleId, UpdateFraudRuleRequest request) {
        FraudRule rule = fraudRuleRepository.findById(ruleId)
            .orElseThrow(() -> new RuleNotFoundException(ruleId));

        if (request.getRuleName() != null && !request.getRuleName().isBlank()) {
            rule.setRuleName(request.getRuleName().trim());
        }
        if (request.getRuleType() != null && !request.getRuleType().isBlank()) {
            rule.setRuleType(request.getRuleType().trim().toUpperCase());
        }
        if (request.getConditions() != null && !request.getConditions().isEmpty()) {
            rule.setConditions(request.getConditions());
        }
        if (request.getAction() != null && !request.getAction().isBlank()) {
            rule.setAction(request.getAction().trim().toUpperCase());
        }
        if (request.getScoreImpact() != null) {
            rule.setScoreImpact(request.getScoreImpact());
        }
        if (request.getPriority() != null) {
            rule.setPriority(request.getPriority());
        }
        if (request.getIsActive() != null) {
            rule.setIsActive(request.getIsActive());
        }

        FraudRule saved = fraudRuleRepository.save(rule);
        log.info("Updated fraud rule: id={}, name={}", saved.getId(), saved.getRuleName());
        return toResponse(saved);
    }

    @Transactional
    public void deleteRule(UUID ruleId) {
        FraudRule rule = fraudRuleRepository.findById(ruleId)
            .orElseThrow(() -> new RuleNotFoundException(ruleId));
        fraudRuleRepository.delete(rule);
        log.warn("Deleted fraud rule: id={}, name={}", rule.getId(), rule.getRuleName());
    }

    private FraudRuleResponse toResponse(FraudRule rule) {
        return FraudRuleResponse.builder()
                .id(rule.getId())
                .ruleName(rule.getRuleName())
                .ruleType(rule.getRuleType())
                .conditions(rule.getConditions())
                .action(rule.getAction())
                .scoreImpact(rule.getScoreImpact())
                .priority(rule.getPriority())
                .isActive(rule.getIsActive())
                .createdBy(rule.getCreatedBy())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
