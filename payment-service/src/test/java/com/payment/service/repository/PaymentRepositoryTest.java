package com.payment.service.repository;

import com.payment.service.entity.Money;
import com.payment.service.entity.Payment;
import com.payment.service.entity.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PaymentRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Test
    void shouldSaveAndFindPayment() {
        // Given
        Money amount = Money.of(new BigDecimal("100.00"), "USD");
        Payment payment = Payment.builder()
            .merchantId(UUID.randomUUID())
            .customerId(UUID.randomUUID())
            .amount(amount)
            .status(PaymentStatus.PENDING)
            .idempotencyKey("test_key_123")
            .createdAt(Instant.now())
            .build();
        
        // When
        Payment saved = paymentRepository.save(payment);
        entityManager.flush();
        entityManager.clear(); // Clear persistence context
        
        Optional<Payment> found = paymentRepository.findById(saved.getId());
        
        // Then
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(PaymentStatus.PENDING, found.get().getStatus());
        assertEquals(new BigDecimal("100.00"), found.get().getAmount().getAmount());
    }
    
    @Test
    void shouldFindByIdempotencyKey() {
        // Given
        String idempotencyKey = "unique_key_456";
        Payment payment = Payment.builder()
            .merchantId(UUID.randomUUID())
            .amount(Money.of(new BigDecimal("50.00"), "USD"))
            .status(PaymentStatus.PENDING)
            .idempotencyKey(idempotencyKey)
            .createdAt(Instant.now())
            .build();
        
        paymentRepository.save(payment);
        entityManager.flush();
        
        // When
        Optional<Payment> found = paymentRepository.findByIdempotencyKey(idempotencyKey);
        
        // Then
        assertTrue(found.isPresent());
        assertEquals(idempotencyKey, found.get().getIdempotencyKey());
    }
    
    @Test
    void shouldFindByMerchantId() {
        // Given
        UUID merchantId = UUID.randomUUID();
        
        Payment payment1 = createPayment(merchantId, "100.00", PaymentStatus.CAPTURED);
        Payment payment2 = createPayment(merchantId, "200.00", PaymentStatus.AUTHORIZED);
        Payment payment3 = createPayment(UUID.randomUUID(), "300.00", PaymentStatus.CAPTURED);
        
        paymentRepository.saveAll(List.of(payment1, payment2, payment3));
        entityManager.flush();
        
        // When
        List<Payment> merchantPayments = paymentRepository.findByMerchantId(merchantId);
        
        // Then
        assertEquals(2, merchantPayments.size());
        assertTrue(merchantPayments.stream()
            .allMatch(p -> p.getMerchantId().equals(merchantId)));
    }
    
    @Test
    void shouldCountByStatus() {
        // Given
        UUID merchantId = UUID.randomUUID();
        
        paymentRepository.save(createPayment(merchantId, "100", PaymentStatus.CAPTURED));
        paymentRepository.save(createPayment(merchantId, "200", PaymentStatus.CAPTURED));
        paymentRepository.save(createPayment(merchantId, "300", PaymentStatus.FAILED));
        
        entityManager.flush();
        
        // When
        long capturedCount = paymentRepository.countByStatus(PaymentStatus.CAPTURED);
        long failedCount = paymentRepository.countByStatus(PaymentStatus.FAILED);
        
        // Then
        assertEquals(2, capturedCount);
        assertEquals(1, failedCount);
    }
    
    @Test
    void shouldFindByIdForUpdate() {
        // Given
        Payment payment = createPayment(UUID.randomUUID(), "100", PaymentStatus.AUTHORIZED);
        Payment saved = paymentRepository.save(payment);
        entityManager.flush();
        
        // When
        Optional<Payment> found = paymentRepository.findByIdForUpdate(saved.getId());
        
        // Then
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }
    
    private Payment createPayment(UUID merchantId, String amount, PaymentStatus status) {
        return Payment.builder()
            .merchantId(merchantId)
            .customerId(UUID.randomUUID())
            .amount(Money.of(new BigDecimal(amount), "USD"))
            .status(status)
            .idempotencyKey(UUID.randomUUID().toString())
            .createdAt(Instant.now())
            .build();
    }
}