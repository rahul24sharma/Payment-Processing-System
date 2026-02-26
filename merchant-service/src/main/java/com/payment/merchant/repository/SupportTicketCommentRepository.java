package com.payment.merchant.repository;

import com.payment.merchant.entity.SupportTicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupportTicketCommentRepository extends JpaRepository<SupportTicketComment, UUID> {
    List<SupportTicketComment> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
