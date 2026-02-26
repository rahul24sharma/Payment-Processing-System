package com.payment.ledger.service;

import com.payment.ledger.dto.CreateLedgerEntryGroupRequest;
import com.payment.ledger.dto.LedgerEntryGroupResponse;
import com.payment.ledger.dto.ReconciliationReportResponse;
import com.payment.ledger.entity.AccountBalance;
import com.payment.ledger.entity.AccountType;
import com.payment.ledger.entity.LedgerEntry;
import com.payment.ledger.repository.AccountBalanceRepository;
import com.payment.ledger.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class LedgerService {
    
    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    
    private static final BigDecimal PLATFORM_FEE_PERCENT = new BigDecimal("0.029"); // 2.9%
    
    /**
     * Record payment capture in ledger (double-entry bookkeeping)
     */
    public void recordPaymentCapture(Map<String, Object> event) {
        UUID paymentId = UUID.fromString((String) event.get("paymentId"));
        UUID merchantId = UUID.fromString((String) event.get("merchantId"));
        UUID customerId = UUID.fromString((String) event.get("customerId"));
        
        Number amountNumber = (Number) event.get("amount");
        long amountInCents = amountNumber.longValue();
        BigDecimal amount = BigDecimal.valueOf(amountInCents).divide(BigDecimal.valueOf(100));
        
        String currency = (String) event.get("currency");
        
        log.info("Recording payment capture: paymentId={}, amount={}, currency={}", 
            paymentId, amount, currency);
        
        UUID entryGroupId = UUID.randomUUID();
        
        // Calculate fees
        BigDecimal platformFee = amount.multiply(PLATFORM_FEE_PERCENT)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal merchantAmount = amount.subtract(platformFee);
        
        List<LedgerEntry> entries = new ArrayList<>();
        
        // Entry 1: Debit Customer Account (money leaving customer)
        entries.add(LedgerEntry.builder()
            .entryGroupId(entryGroupId)
            .accountId(customerId)
            .accountType(AccountType.CUSTOMER)
            .debitAmount(amount)
            .creditAmount(BigDecimal.ZERO)
            .currency(currency)
            .paymentId(paymentId)
            .description("Payment to merchant")
            .build());
        
        // Entry 2: Credit Merchant Account (money to merchant)
        entries.add(LedgerEntry.builder()
            .entryGroupId(entryGroupId)
            .accountId(merchantId)
            .accountType(AccountType.MERCHANT)
            .debitAmount(BigDecimal.ZERO)
            .creditAmount(merchantAmount)
            .currency(currency)
            .paymentId(paymentId)
            .description("Payment received (net of fees)")
            .build());
        
        // Entry 3: Credit Platform Fee Account
        UUID platformAccountId = getPlatformAccountId(currency);
        entries.add(LedgerEntry.builder()
            .entryGroupId(entryGroupId)
            .accountId(platformAccountId)
            .accountType(AccountType.PLATFORM_FEE)
            .debitAmount(BigDecimal.ZERO)
            .creditAmount(platformFee)
            .currency(currency)
            .paymentId(paymentId)
            .description("Platform processing fee")
            .build());
        
        // Save all entries atomically
        ledgerEntryRepository.saveAll(entries);
        
        // Validate books balance
        validateBalance(entryGroupId);
        
        // Update account balances
        updateBalance(customerId, AccountType.CUSTOMER, amount.negate(), currency); // Subtract from customer
        updateBalance(merchantId, AccountType.MERCHANT, merchantAmount, currency); // Add to merchant
        updateBalance(platformAccountId, AccountType.PLATFORM_FEE, platformFee, currency); // Add fee
        
        log.info("Payment capture recorded in ledger: paymentId={}, entries={}", 
            paymentId, entries.size());
    }
    
    /**
     * Record refund in ledger (reverses original entries)
     */
    public void recordRefund(Map<String, Object> event) {
        UUID paymentId = UUID.fromString((String) event.get("paymentId"));
        UUID merchantId = UUID.fromString((String) event.get("merchantId"));
        UUID customerId = UUID.fromString((String) event.get("customerId"));
        
        // Get refund details from metadata
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) event.get("metadata");
        Number refundAmountNumber = (Number) metadata.get("refundAmount");
        long refundAmountInCents = refundAmountNumber.longValue();
        BigDecimal refundAmount = BigDecimal.valueOf(refundAmountInCents)
            .divide(BigDecimal.valueOf(100));
        
        String currency = (String) event.get("currency");
        UUID refundId = UUID.randomUUID(); // In production, get from event
        
        log.info("Recording refund: paymentId={}, amount={}", paymentId, refundAmount);
        
        UUID entryGroupId = UUID.randomUUID();
        
        // Calculate fee refund
        BigDecimal platformFeeRefund = refundAmount.multiply(PLATFORM_FEE_PERCENT)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal merchantRefund = refundAmount.subtract(platformFeeRefund);
        
        List<LedgerEntry> entries = new ArrayList<>();
        
        // Entry 1: Credit Customer Account (money back to customer)
        entries.add(LedgerEntry.builder()
            .entryGroupId(entryGroupId)
            .accountId(customerId)
            .accountType(AccountType.CUSTOMER)
            .debitAmount(BigDecimal.ZERO)
            .creditAmount(refundAmount)
            .currency(currency)
            .paymentId(paymentId)
            .refundId(refundId)
            .description("Refund from merchant")
            .build());
        
        // Entry 2: Debit Merchant Account (money from merchant)
        entries.add(LedgerEntry.builder()
            .entryGroupId(entryGroupId)
            .accountId(merchantId)
            .accountType(AccountType.MERCHANT)
            .debitAmount(merchantRefund)
            .creditAmount(BigDecimal.ZERO)
            .currency(currency)
            .paymentId(paymentId)
            .refundId(refundId)
            .description("Refund issued")
            .build());
        
        // Entry 3: Debit Platform Fee Account
        UUID platformAccountId = getPlatformAccountId(currency);
        entries.add(LedgerEntry.builder()
            .entryGroupId(entryGroupId)
            .accountId(platformAccountId)
            .accountType(AccountType.PLATFORM_FEE)
            .debitAmount(platformFeeRefund)
            .creditAmount(BigDecimal.ZERO)
            .currency(currency)
            .paymentId(paymentId)
            .refundId(refundId)
            .description("Platform fee refund")
            .build());
        
        ledgerEntryRepository.saveAll(entries);
        validateBalance(entryGroupId);
        
        // Update balances
        updateBalance(customerId, AccountType.CUSTOMER, refundAmount, currency);
        updateBalance(merchantId, AccountType.MERCHANT, merchantRefund.negate(), currency);
        updateBalance(platformAccountId, AccountType.PLATFORM_FEE, platformFeeRefund.negate(), currency);
        
        log.info("Refund recorded in ledger: paymentId={}, refundAmount={}", 
            paymentId, refundAmount);
    }
    
    /**
     * Validate that debits equal credits for an entry group
     */
    private void validateBalance(UUID entryGroupId) {
        List<LedgerEntry> entries = ledgerEntryRepository.findByEntryGroupId(entryGroupId);
        
        BigDecimal totalDebits = entries.stream()
            .map(LedgerEntry::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredits = entries.stream()
            .map(LedgerEntry::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalDebits.compareTo(totalCredits) != 0) {
            String errorMsg = String.format(
                "LEDGER IMBALANCE DETECTED! EntryGroup=%s, Debits=%s, Credits=%s",
                entryGroupId, totalDebits, totalCredits
            );
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        log.debug("Ledger balanced: entryGroup={}, amount={}", entryGroupId, totalDebits);
    }
    
    /**
     * Update account balance (materialized view)
     */
    @Transactional
    private void updateBalance(UUID accountId, AccountType accountType, 
                              BigDecimal amountChange, String currency) {
        
        AccountBalance balance = accountBalanceRepository.findByAccountIdForUpdate(accountId)
            .orElseGet(() -> AccountBalance.builder()
                .accountId(accountId)
                .accountType(accountType)
                .balance(BigDecimal.ZERO)
                .currency(currency)
                .lastUpdated(Instant.now())
                .build());
        
        balance.addToBalance(amountChange);
        accountBalanceRepository.save(balance);
        
        log.debug("Updated balance: accountId={}, newBalance={}", accountId, balance.getBalance());
    }
    
    /**
     * Get platform account ID for a currency
     */
    private UUID getPlatformAccountId(String currency) {
        // Deterministic UUID based on currency
        return UUID.nameUUIDFromBytes(("platform-fee-" + currency).getBytes());
    }
    
    /**
     * Get account balance
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID accountId) {
        return accountBalanceRepository.findByAccountId(accountId)
            .map(AccountBalance::getBalance)
            .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Get ledger entries for an account
     */
    @Transactional(readOnly = true)
    public List<LedgerEntry> getAccountEntries(UUID accountId) {
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    /**
     * Manually create a balanced ledger entry group.
     */
    public LedgerEntryGroupResponse createEntryGroup(CreateLedgerEntryGroupRequest request) {
        UUID entryGroupId = UUID.randomUUID();
        List<LedgerEntry> entries = new ArrayList<>();

        for (CreateLedgerEntryGroupRequest.EntryLine line : request.getEntries()) {
            BigDecimal debit = line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO;
            BigDecimal credit = line.getCreditAmount() != null ? line.getCreditAmount() : BigDecimal.ZERO;

            LedgerEntry entry = LedgerEntry.builder()
                .entryGroupId(entryGroupId)
                .accountId(line.getAccountId())
                .accountType(line.getAccountType())
                .debitAmount(debit)
                .creditAmount(credit)
                .currency(request.getCurrency())
                .paymentId(request.getPaymentId())
                .refundId(request.getRefundId())
                .settlementId(request.getSettlementId())
                .description(line.getDescription() != null ? line.getDescription() : request.getDescription())
                .build();

            entries.add(entry);
        }

        List<LedgerEntry> saved = ledgerEntryRepository.saveAll(entries);
        validateBalance(entryGroupId);

        for (LedgerEntry entry : saved) {
            BigDecimal netChange = entry.getCreditAmount().subtract(entry.getDebitAmount());
            updateBalance(entry.getAccountId(), entry.getAccountType(), netChange, entry.getCurrency());
        }

        BigDecimal totalDebits = saved.stream()
            .map(LedgerEntry::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = saved.stream()
            .map(LedgerEntry::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return LedgerEntryGroupResponse.builder()
            .entryGroupId(entryGroupId)
            .currency(request.getCurrency())
            .paymentId(request.getPaymentId())
            .refundId(request.getRefundId())
            .settlementId(request.getSettlementId())
            .description(request.getDescription())
            .totalDebits(totalDebits)
            .totalCredits(totalCredits)
            .entryCount(saved.size())
            .ledgerEntryIds(saved.stream().map(LedgerEntry::getId).toList())
            .build();
    }

    /**
     * Basic reconciliation report for a time window.
     */
    @Transactional(readOnly = true)
    public ReconciliationReportResponse getReconciliationReport(Instant start, Instant end) {
        List<LedgerEntry> entries = ledgerEntryRepository.findEntriesBetween(start, end);
        List<UUID> entryGroups = ledgerEntryRepository.findEntryGroupsByDateRange(start, end);

        BigDecimal totalDebits = entries.stream()
            .map(LedgerEntry::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = entries.stream()
            .map(LedgerEntry::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal difference = totalDebits.subtract(totalCredits);

        return ReconciliationReportResponse.builder()
            .start(start)
            .end(end)
            .totalEntries(entries.size())
            .totalEntryGroups(entryGroups.size())
            .totalDebits(totalDebits)
            .totalCredits(totalCredits)
            .balanced(difference.compareTo(BigDecimal.ZERO) == 0)
            .difference(difference)
            .build();
    }
}
