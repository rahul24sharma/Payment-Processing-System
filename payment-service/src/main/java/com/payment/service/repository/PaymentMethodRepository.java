package com.payment.service.repository;

import com.payment.service.entity.PaymentMethod;
import com.payment.service.entity.PaymentMethodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
    
    /**
     * Find all payment methods for a customer
     */
    List<PaymentMethod> findByCustomerIdAndIsActiveTrue(UUID customerId);
    
    /**
     * Find default payment method for customer
     */
    Optional<PaymentMethod> findByCustomerIdAndIsDefaultTrueAndIsActiveTrue(UUID customerId);
    
    /**
     * Find payment method by token
     */
    Optional<PaymentMethod> findByToken(String token);
    
    /**
     * Find active payment methods by type
     */
    List<PaymentMethod> findByCustomerIdAndTypeAndIsActiveTrue(
        UUID customerId, 
        PaymentMethodType type
    );
    
    /**
     * Check if customer has any active payment methods
     */
    boolean existsByCustomerIdAndIsActiveTrue(UUID customerId);
    
    /**
     * Find non-expired card payment methods
     */
    @Query("""
        SELECT pm FROM PaymentMethod pm 
        WHERE pm.customerId = :customerId 
        AND pm.type = 'CARD'
        AND pm.isActive = true
        AND (pm.cardExpYear > :currentYear 
             OR (pm.cardExpYear = :currentYear AND pm.cardExpMonth >= :currentMonth))
        """)
    List<PaymentMethod> findValidCards(
        @Param("customerId") UUID customerId,
        @Param("currentYear") int currentYear,
        @Param("currentMonth") int currentMonth
    );
}