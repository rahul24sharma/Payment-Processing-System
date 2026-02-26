package com.payment.settlement.repository;

import com.payment.settlement.entity.Payout;
import com.payment.settlement.entity.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {
    
    List<Payout> findByMerchantIdOrderBySettlementDateDesc(UUID merchantId);

    List<Payout> findByMerchantIdAndSettlementDateBetweenOrderBySettlementDateDesc(
        UUID merchantId,
        LocalDate start,
        LocalDate end
    );
    
    List<Payout> findByStatus(PayoutStatus status);
    
    @Query("""
        SELECT p FROM Payout p 
        WHERE p.settlementDate BETWEEN :start AND :end
        ORDER BY p.settlementDate DESC
        """)
    List<Payout> findBySettlementDateBetween(
        @Param("start") LocalDate start,
        @Param("end") LocalDate end
    );
}
