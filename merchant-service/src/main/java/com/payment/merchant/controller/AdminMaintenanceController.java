package com.payment.merchant.controller;

import com.payment.merchant.service.BankAccountReencryptionService;
import com.payment.merchant.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/maintenance")
@Tag(name = "Admin Maintenance", description = "Operational maintenance endpoints")
@RequiredArgsConstructor
@Slf4j
public class AdminMaintenanceController {

    private final BankAccountReencryptionService bankAccountReencryptionService;
    private final AuditLogService auditLogService;

    @Value("${merchant.security.admin-maintenance-token:}")
    private String adminMaintenanceToken;

    @Operation(summary = "Re-encrypt merchant bank account fields using the active encryption key")
    @PostMapping("/reencrypt-bank-accounts")
    public ResponseEntity<?> reencryptBankAccounts(
        @RequestHeader(value = "X-Admin-Maintenance-Token", required = false) String providedToken,
        @RequestParam(defaultValue = "true") boolean dryRun,
        @RequestParam(defaultValue = "500") int maxMerchants,
        @RequestParam(defaultValue = "100") int pageSize,
        HttpServletRequest httpRequest
    ) {
        requireMaintenanceToken(providedToken);

        var result = bankAccountReencryptionService.reencryptExistingBankAccounts(dryRun, maxMerchants, pageSize);
        log.warn(
            "Bank account re-encryption maintenance executed: dryRun={}, scanned={}, updated={}, failed={}",
            result.isDryRun(), result.getScanned(), result.getUpdated(), result.getFailed()
        );
        auditLogService.log(
            null, "SYSTEM", "maintenance-token", "ADMIN",
            "BANK_ACCOUNT_REENCRYPTION_RUN", "MERCHANT_BANK_ACCOUNT", null, "SUCCESS",
            Map.of(
                "dryRun", dryRun,
                "maxMerchants", maxMerchants,
                "pageSize", pageSize,
                "scanned", result.getScanned(),
                "updated", result.getUpdated(),
                "failed", result.getFailed(),
                "activeKeyId", result.getActiveKeyId()
            ),
            httpRequest
        );
        return ResponseEntity.ok(result);
    }

    private void requireMaintenanceToken(String providedToken) {
        if (adminMaintenanceToken == null || adminMaintenanceToken.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Admin maintenance token is not configured"
            );
        }
        if (providedToken == null || !adminMaintenanceToken.equals(providedToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid admin maintenance token");
        }
    }
}
