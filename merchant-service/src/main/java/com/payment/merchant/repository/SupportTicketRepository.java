package com.payment.merchant.repository;

import com.payment.merchant.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {
    List<SupportTicket> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
    List<SupportTicket> findByMerchantIdAndStatusOrderByCreatedAtDesc(UUID merchantId, String status);
    Optional<SupportTicket> findByIdAndMerchantId(UUID id, UUID merchantId);
}
