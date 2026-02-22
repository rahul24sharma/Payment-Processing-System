package com.payment.settlement.repository;

import com.payment.settlement.entity.SettlementBatch;
import com.payment.settlement.entity.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, UUID> {
    
    Optional<SettlementBatch> findBySettlementDate(LocalDate settlementDate);
    
    List<SettlementBatch> findByStatusOrderBySettlementDateDesc(SettlementStatus status);
    
    List<SettlementBatch> findTop10ByOrderBySettlementDateDesc();
}