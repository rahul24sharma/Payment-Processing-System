package com.payment.merchant.service;

import com.payment.merchant.entity.Merchant;
import com.payment.merchant.repository.MerchantRepository;
import com.payment.merchant.security.BankAccountCryptoService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankAccountReencryptionService {

    private final MerchantRepository merchantRepository;
    private final BankAccountCryptoService cryptoService;

    @Transactional
    public Result reencryptExistingBankAccounts(boolean dryRun, int maxMerchants, int pageSize) {
        if (!cryptoService.isConfigured()) {
            throw new IllegalStateException("Bank account encryption is not configured");
        }

        int safeMax = Math.max(1, maxMerchants);
        int safePageSize = Math.max(1, Math.min(pageSize, 500));

        int scanned = 0;
        int withBankAccount = 0;
        int updated = 0;
        int unchanged = 0;
        int failed = 0;

        int pageIndex = 0;
        while (scanned < safeMax) {
            Page<Merchant> page = merchantRepository.findAll(PageRequest.of(pageIndex, safePageSize));
            if (page.isEmpty()) {
                break;
            }

            List<Merchant> content = page.getContent();
            for (Merchant merchant : content) {
                if (scanned >= safeMax) {
                    break;
                }
                scanned++;

                try {
                    if (processMerchant(merchant, dryRun)) {
                        updated++;
                        withBankAccount++;
                    } else {
                        if (hasBankAccount(merchant)) {
                            withBankAccount++;
                            unchanged++;
                        }
                    }
                } catch (Exception e) {
                    failed++;
                    log.error("Bank account re-encryption failed for merchantId={}", merchant.getId(), e);
                }
            }

            if (!page.hasNext()) {
                break;
            }
            pageIndex++;
        }

        return Result.builder()
            .dryRun(dryRun)
            .activeKeyId(cryptoService.getActiveKeyId())
            .scanned(scanned)
            .withBankAccount(withBankAccount)
            .updated(updated)
            .unchanged(unchanged)
            .failed(failed)
            .build();
    }

    @SuppressWarnings("unchecked")
    private boolean processMerchant(Merchant merchant, boolean dryRun) {
        if (!hasBankAccount(merchant)) {
            return false;
        }

        Map<String, Object> settings = merchant.getSettings();
        Object bankAccountObj = settings.get("bankAccount");
        if (!(bankAccountObj instanceof Map<?, ?> rawBankMap)) {
            return false;
        }

        Map<String, Object> bankAccount = new HashMap<>((Map<String, Object>) rawBankMap);

        boolean changed = false;
        changed |= reencryptField(bankAccount, "accountNumber");
        changed |= reencryptField(bankAccount, "routingNumber");

        String accountNumber = stringValue(bankAccount.get("accountNumber"));
        String routingNumber = stringValue(bankAccount.get("routingNumber"));

        if (accountNumber != null && !accountNumber.isBlank()) {
            String accountLast4 = cryptoService.decryptIfEncrypted(accountNumber);
            bankAccount.put("accountNumberLast4", last4(accountLast4));
        }
        if (routingNumber != null && !routingNumber.isBlank()) {
            String routingLast4 = cryptoService.decryptIfEncrypted(routingNumber);
            bankAccount.put("routingNumberLast4", last4(routingLast4));
        }

        if (!changed) {
            return false;
        }

        if (!dryRun) {
            settings.put("bankAccount", bankAccount);
            merchantRepository.save(merchant);
            log.info("Re-encrypted merchant bank account fields: merchantId={}", merchant.getId());
        } else {
            log.info("Dry-run re-encryption candidate: merchantId={}", merchant.getId());
        }
        return true;
    }

    private boolean reencryptField(Map<String, Object> bankAccount, String field) {
        String raw = stringValue(bankAccount.get(field));
        if (raw == null || raw.isBlank()) {
            return false;
        }
        if (!cryptoService.requiresReencryption(raw)) {
            return false;
        }
        bankAccount.put(field, cryptoService.reencryptToActive(raw));
        return true;
    }

    private boolean hasBankAccount(Merchant merchant) {
        return merchant.getSettings() != null && merchant.getSettings().get("bankAccount") instanceof Map<?, ?>;
    }

    private String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private String last4(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.length() <= 4) {
            return value;
        }
        return value.substring(value.length() - 4);
    }

    @Value
    @Builder
    public static class Result {
        boolean dryRun;
        String activeKeyId;
        int scanned;
        int withBankAccount;
        int updated;
        int unchanged;
        int failed;
    }
}
