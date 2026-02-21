package com.payment.service.repository;

import com.payment.service.entity.Payment;
import com.payment.service.entity.PaymentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@Slf4j
public class CustomPaymentRepositoryImpl implements CustomPaymentRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public PaymentStatistics getStatistics(UUID merchantId, Instant startDate, Instant endDate) {
        String query = """
            SELECT 
                COUNT(p.id) as totalCount,
                COALESCE(SUM(p.amount), 0) as totalAmount,
                COALESCE(AVG(p.amount), 0) as averageAmount,
                COUNT(CASE WHEN p.status = 'AUTHORIZED' THEN 1 END) as authorizedCount,
                COUNT(CASE WHEN p.status = 'CAPTURED' THEN 1 END) as capturedCount,
                COUNT(CASE WHEN p.status = 'FAILED' THEN 1 END) as failedCount
            FROM payments p
            WHERE p.merchant_id = :merchantId
            AND p.created_at BETWEEN :startDate AND :endDate
            """;
        
        Object[] result = (Object[]) entityManager.createNativeQuery(query)
            .setParameter("merchantId", merchantId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .getSingleResult();
        
        return new PaymentStatistics(
            ((Number) result[0]).longValue(),
            (BigDecimal) result[1],
            (BigDecimal) result[2],
            ((Number) result[3]).longValue(),
            ((Number) result[4]).longValue(),
            ((Number) result[5]).longValue()
        );
    }
    
    @Override
    public List<Payment> findWithRefunds(UUID merchantId, int limit) {
        String jpql = """
            SELECT DISTINCT p FROM Payment p 
            LEFT JOIN FETCH p.refunds 
            WHERE p.merchantId = :merchantId 
            ORDER BY p.createdAt DESC
            """;
        
        return entityManager.createQuery(jpql, Payment.class)
            .setParameter("merchantId", merchantId)
            .setMaxResults(limit)
            .getResultList();
    }
    
    @Override
    public List<Payment> searchPayments(PaymentSearchCriteria criteria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Payment> query = cb.createQuery(Payment.class);
        Root<Payment> payment = query.from(Payment.class);
        
        List<Predicate> predicates = new ArrayList<>();
        
        // Merchant ID (required)
        if (criteria.merchantId() != null) {
            predicates.add(cb.equal(payment.get("merchantId"), criteria.merchantId()));
        }
        
        // Customer ID (optional)
        if (criteria.customerId() != null) {
            predicates.add(cb.equal(payment.get("customerId"), criteria.customerId()));
        }
        
        // Status (optional)
        if (criteria.status() != null) {
            predicates.add(cb.equal(payment.get("status"), criteria.status()));
        }
        
        // Amount range (optional)
        if (criteria.minAmount() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                payment.get("amount").get("amount"), 
                criteria.minAmount()
            ));
        }
        if (criteria.maxAmount() != null) {
            predicates.add(cb.lessThanOrEqualTo(
                payment.get("amount").get("amount"), 
                criteria.maxAmount()
            ));
        }
        
        // Date range (optional)
        if (criteria.startDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                payment.get("createdAt"), 
                criteria.startDate()
            ));
        }
        if (criteria.endDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(
                payment.get("createdAt"), 
                criteria.endDate()
            ));
        }
        
        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(payment.get("createdAt")));
        
        return entityManager.createQuery(query).getResultList();
    }
}