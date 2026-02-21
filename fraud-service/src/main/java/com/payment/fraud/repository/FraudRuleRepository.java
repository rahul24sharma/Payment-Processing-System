package com.payment.fraud.repository;

import com.payment.fraud.entity.FraudRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRule, UUID> {
    
    List<FraudRule> findByIsActiveTrueOrderByPriorityDesc();
    
    List<FraudRule> findByRuleTypeAndIsActiveTrue(String ruleType);
    
    @Query("SELECT r FROM FraudRule r WHERE r.isActive = true ORDER BY r.priority DESC")
    List<FraudRule> findActiveRulesByPriority();
}