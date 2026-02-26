package com.payment.settlement.service;

import com.payment.settlement.client.PaymentServiceClient;
import com.payment.settlement.dto.ReserveReportResponse;
import com.payment.settlement.dto.PaymentDTO;
import com.payment.settlement.dto.SettlementReportResponse;
import com.payment.settlement.entity.*;
import com.payment.settlement.repository.PayoutRepository;
import com.payment.settlement.repository.SettlementBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class SettlementService {
    
    private final SettlementBatchRepository settlementBatchRepository;
    private final PayoutRepository payoutRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final BankTransferService bankTransferService;
    
    private static final BigDecimal PLATFORM_FEE_PERCENT = new BigDecimal("0.029"); // 2.9%
    private static final BigDecimal RESERVE_PERCENT = new BigDecimal("0.05"); // 5% reserve
    
    /**
     * Process settlement for a date (T+2 settlement)
     * Called by scheduled job daily
     */
    public SettlementBatch processSettlement(LocalDate settlementDate) {
        log.info("Starting settlement for date: {}", settlementDate);
        
        // Check if already processed
        Optional<SettlementBatch> existing = settlementBatchRepository
            .findBySettlementDate(settlementDate);
        
        if (existing.isPresent()) {
            log.warn("Settlement already processed for date: {}", settlementDate);
            return existing.get();
        }
        
        // Calculate T-2 date (payments captured 2 days ago)
        LocalDate captureDate = settlementDate.minusDays(2);
        
        log.info("Processing payments captured on: {} (T-2)", captureDate);
        
        // Get date range
        Instant startOfDay = captureDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = captureDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        
        // Fetch captured payments from Payment Service
        List<PaymentDTO> payments = paymentServiceClient.getCapturedPayments(startOfDay, endOfDay);
        
        log.info("Found {} payments to settle", payments.size());
        
        // Create settlement batch
        SettlementBatch batch = SettlementBatch.builder()
            .settlementDate(settlementDate)
            .captureDate(captureDate)
            .totalPayments(payments.size())
            .status(SettlementStatus.PROCESSING)
            .build();
        
        batch = settlementBatchRepository.save(batch);
        
        if (payments.isEmpty()) {
            log.info("No payments to settle");
            batch.setStatus(SettlementStatus.COMPLETED);
            batch.setCompletedAt(Instant.now());
            return settlementBatchRepository.save(batch);
        }
        
        // Group payments by merchant
        Map<UUID, List<PaymentDTO>> paymentsByMerchant = payments.stream()
            .collect(Collectors.groupingBy(PaymentDTO::getMerchantId));
        
        log.info("Processing payouts for {} merchants", paymentsByMerchant.size());
        
        // Create payout for each merchant
        for (Map.Entry<UUID, List<PaymentDTO>> entry : paymentsByMerchant.entrySet()) {
            UUID merchantId = entry.getKey();
            List<PaymentDTO> merchantPayments = entry.getValue();
            
            try {
                Payout payout = createPayout(batch, merchantId, merchantPayments, settlementDate);
                batch.addPayout(payout);
                
            } catch (Exception e) {
                log.error("Failed to create payout for merchant: {}", merchantId, e);
                // Continue with other merchants
            }
        }
        
        settlementBatchRepository.save(batch);
        
        // Process all payouts
        for (Payout payout : batch.getPayouts()) {
            try {
                processPayout(payout);
            } catch (Exception e) {
                log.error("Failed to process payout: {}", payout.getId(), e);
                payout.markFailed(e.getMessage());
                payoutRepository.save(payout);
            }
        }
        
        // Update batch status
        long successfulPayouts = batch.getPayouts().stream()
            .filter(p -> p.getStatus() == PayoutStatus.COMPLETED)
            .count();
        
        if (successfulPayouts == batch.getPayouts().size()) {
            batch.setStatus(SettlementStatus.COMPLETED);
        } else if (successfulPayouts > 0) {
            batch.setStatus(SettlementStatus.PARTIALLY_COMPLETED);
        } else {
            batch.setStatus(SettlementStatus.FAILED);
        }
        
        batch.setCompletedAt(Instant.now());
        batch = settlementBatchRepository.save(batch);
        
        log.info("Settlement completed: batchId={}, status={}, successful={}/{}", 
            batch.getId(), batch.getStatus(), successfulPayouts, batch.getTotalPayouts());
        
        return batch;
    }
    
    /**
     * Create a payout for a merchant
     */
    private Payout createPayout(
            SettlementBatch batch,
            UUID merchantId, 
            List<PaymentDTO> payments,
            LocalDate settlementDate) {
        
        log.info("Creating payout for merchant: merchantId={}, payments={}", 
            merchantId, payments.size());
        
        // Calculate amounts
        BigDecimal totalAmount = payments.stream()
            .map(PaymentDTO::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal feeAmount = totalAmount.multiply(PLATFORM_FEE_PERCENT)
            .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal netAmount = totalAmount.subtract(feeAmount);
        
        BigDecimal reserveAmount = netAmount.multiply(RESERVE_PERCENT)
            .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal payoutAmount = netAmount.subtract(reserveAmount);
        
        String currency = payments.get(0).getCurrency();
        
        // Create payout
        Payout payout = Payout.builder()
            .batch(batch)
            .merchantId(merchantId)
            .settlementDate(settlementDate)
            .totalAmount(totalAmount)
            .feeAmount(feeAmount)
            .netAmount(netAmount)
            .reserveAmount(reserveAmount)
            .payoutAmount(payoutAmount)
            .currency(currency)
            .paymentCount(payments.size())
            .status(PayoutStatus.PENDING)
            .build();
        
        payout = payoutRepository.save(payout);
        
        // Link payments to payout
        for (PaymentDTO payment : payments) {
            PayoutPayment link = PayoutPayment.builder()
                .payout(payout)
                .paymentId(payment.getId())
                .build();
            payout.addPayment(link);
        }
        
        payout = payoutRepository.save(payout);
        
        log.info("Payout created: id={}, amount={}, payments={}", 
            payout.getId(), payout.getPayoutAmount(), payments.size());
        
        return payout;
    }
    
    /**
     * Process a payout (transfer money to merchant's bank)
     */
    private void processPayout(Payout payout) {
        log.info("Processing payout: id={}, merchantId={}, amount={}", 
            payout.getId(), payout.getMerchantId(), payout.getPayoutAmount());
        
        payout.setStatus(PayoutStatus.PROCESSING);
        payoutRepository.save(payout);
        
        try {
            // Initiate bank transfer
            String transferId = bankTransferService.transfer(
                payout.getMerchantId(),
                payout.getPayoutAmount(),
                payout.getCurrency(),
                "Payout-" + payout.getId()
            );
            
            payout.markCompleted(transferId);
            payoutRepository.save(payout);
            
            log.info("Payout completed: id={}, transferId={}", payout.getId(), transferId);
            
        } catch (Exception e) {
            log.error("Payout failed: id={}", payout.getId(), e);
            payout.markFailed(e.getMessage());
            payoutRepository.save(payout);
            throw e;
        }
    }
    
    /**
     * Get settlement history
     */
    @Transactional(readOnly = true)
    public List<SettlementBatch> getSettlementHistory(int limit) {
        return settlementBatchRepository.findTop10ByOrderBySettlementDateDesc();
    }
    
    /**
     * Get payouts for a merchant
     */
    @Transactional(readOnly = true)
    public List<Payout> getMerchantPayouts(UUID merchantId) {
        return payoutRepository.findByMerchantIdOrderBySettlementDateDesc(merchantId);
    }

    /**
     * Settlement report for a date range.
     */
    @Transactional(readOnly = true)
    public SettlementReportResponse getSettlementReport(LocalDate startDate, LocalDate endDate) {
        List<Payout> payouts = payoutRepository.findBySettlementDateBetween(startDate, endDate);
        List<SettlementBatch> batches = settlementBatchRepository.findAll().stream()
            .filter(b -> !b.getSettlementDate().isBefore(startDate) && !b.getSettlementDate().isAfter(endDate))
            .toList();

        BigDecimal totalGrossAmount = sumPayoutField(payouts, Payout::getTotalAmount);
        BigDecimal totalFeeAmount = sumPayoutField(payouts, Payout::getFeeAmount);
        BigDecimal totalNetAmount = sumPayoutField(payouts, Payout::getNetAmount);
        BigDecimal totalReserveAmount = sumPayoutField(payouts, Payout::getReserveAmount);
        BigDecimal totalPayoutAmount = sumPayoutField(payouts, Payout::getPayoutAmount);

        int totalPaymentCount = payouts.stream()
            .map(Payout::getPaymentCount)
            .filter(Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();

        Map<String, Long> payoutStatusCounts = payouts.stream()
            .collect(Collectors.groupingBy(
                p -> p.getStatus().name(),
                TreeMap::new,
                Collectors.counting()
            ));

        Map<String, Long> batchStatusCounts = batches.stream()
            .collect(Collectors.groupingBy(
                b -> b.getStatus().name(),
                TreeMap::new,
                Collectors.counting()
            ));

        return SettlementReportResponse.builder()
            .startDate(startDate)
            .endDate(endDate)
            .batchCount(batches.size())
            .payoutCount(payouts.size())
            .totalPaymentCount(totalPaymentCount)
            .totalGrossAmount(totalGrossAmount)
            .totalFeeAmount(totalFeeAmount)
            .totalNetAmount(totalNetAmount)
            .totalReserveAmount(totalReserveAmount)
            .totalPayoutAmount(totalPayoutAmount)
            .payoutStatusCounts(payoutStatusCounts)
            .batchStatusCounts(batchStatusCounts)
            .build();
    }

    /**
     * Reserve report (optionally per merchant) for a date range.
     */
    @Transactional(readOnly = true)
    public ReserveReportResponse getReserveReport(LocalDate startDate, LocalDate endDate, UUID merchantId) {
        List<Payout> payouts = merchantId == null
            ? payoutRepository.findBySettlementDateBetween(startDate, endDate)
            : payoutRepository.findByMerchantIdAndSettlementDateBetweenOrderBySettlementDateDesc(merchantId, startDate, endDate);

        BigDecimal totalReserveHeld = sumPayoutField(payouts, Payout::getReserveAmount);
        BigDecimal totalPayoutAmount = sumPayoutField(payouts, Payout::getPayoutAmount);
        BigDecimal totalNetAmount = sumPayoutField(payouts, Payout::getNetAmount);

        BigDecimal reserveRatePercentOfNet = BigDecimal.ZERO;
        if (totalNetAmount.compareTo(BigDecimal.ZERO) > 0) {
            reserveRatePercentOfNet = totalReserveHeld
                .multiply(BigDecimal.valueOf(100))
                .divide(totalNetAmount, 4, RoundingMode.HALF_UP);
        }

        return ReserveReportResponse.builder()
            .startDate(startDate)
            .endDate(endDate)
            .merchantId(merchantId)
            .payoutCount(payouts.size())
            .totalReserveHeld(totalReserveHeld)
            .totalPayoutAmount(totalPayoutAmount)
            .totalNetAmount(totalNetAmount)
            .reserveRatePercentOfNet(reserveRatePercentOfNet)
            .build();
    }

    private BigDecimal sumPayoutField(List<Payout> payouts, java.util.function.Function<Payout, BigDecimal> getter) {
        return payouts.stream()
            .map(getter)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
