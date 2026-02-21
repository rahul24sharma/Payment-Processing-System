package com.payment.fraud.repository;

import com.payment.fraud.entity.Blocklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlocklistRepository extends JpaRepository<Blocklist, UUID> {
    
    Optional<Blocklist> findByTypeAndValueAndIsActiveTrue(String type, String value);
    
    boolean existsByTypeAndValueAndIsActiveTrue(String type, String value);
}