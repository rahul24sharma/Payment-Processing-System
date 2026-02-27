package com.payment.ledger.repository;

import com.payment.ledger.entity.ProcessedEventConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventConsumptionRepository extends JpaRepository<ProcessedEventConsumption, String> {
}
