package com.payment.merchant.repository;

import com.payment.merchant.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    
    Optional<Merchant> findByEmail(String email);
    
    Optional<Merchant> findByApiKeyHash(String apiKeyHash);
    
    boolean existsByEmail(String email);
}