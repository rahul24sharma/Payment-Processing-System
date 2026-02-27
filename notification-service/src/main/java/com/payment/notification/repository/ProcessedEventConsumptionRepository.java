package com.payment.notification.repository;

import com.payment.notification.entity.ProcessedEventConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventConsumptionRepository extends JpaRepository<ProcessedEventConsumption, String> {
}
