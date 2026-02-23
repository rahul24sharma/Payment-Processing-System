package com.payment.merchant.repository;

import com.payment.merchant.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    
    List<ApiKey> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
    
    List<ApiKey> findByMerchantIdAndIsActiveTrueOrderByCreatedAtDesc(UUID merchantId);
    
    Optional<ApiKey> findByKeyHash(String keyHash);
    
    boolean existsByKeyHash(String keyHash);
}