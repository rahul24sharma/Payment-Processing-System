package com.payment.merchant.controller;

import com.payment.merchant.dto.ChangePasswordRequest;
import com.payment.merchant.dto.MerchantResponse;
import com.payment.merchant.dto.UpdateBankAccountRequest;
import com.payment.merchant.dto.UpdateNotificationPreferencesRequest;
import com.payment.merchant.dto.UpdateProfileRequest;
import com.payment.merchant.entity.Merchant;
import com.payment.merchant.repository.MerchantRepository;
import com.payment.merchant.security.BankAccountCryptoService;
import com.payment.merchant.security.MerchantRole;
import com.payment.merchant.security.RoleAccessService;
import com.payment.merchant.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchants")
@Tag(name = "Merchants", description = "Merchant account management")
@RequiredArgsConstructor
@Slf4j
public class MerchantController {
    
    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final BankAccountCryptoService bankAccountCryptoService;
    private final RoleAccessService roleAccessService;
    private final AuditLogService auditLogService;
    
    @Operation(summary = "Get current merchant profile")
    @GetMapping("/me")
    public ResponseEntity<MerchantResponse> getProfile(
            @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader) {
        
        UUID merchantId = merchantIdHeader != null 
            ? UUID.fromString(merchantIdHeader) 
            : null;
        
        if (merchantId == null) {
            return ResponseEntity.status(401).build();
        }
        
        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        
        return ResponseEntity.ok(buildMerchantResponse(merchant));
    }
    
    @Operation(summary = "Update merchant profile")
    @PutMapping("/me")
    public ResponseEntity<MerchantResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader,
            HttpServletRequest httpRequest) {
        roleAccessService.requireAny(MerchantRole.ADMIN);
        
        UUID merchantId = merchantIdHeader != null 
            ? UUID.fromString(merchantIdHeader) 
            : null;
        
        if (merchantId == null) {
            return ResponseEntity.status(401).build();
        }
        
        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        
        // Update fields
        if (request.getBusinessName() != null) {
            merchant.setBusinessName(request.getBusinessName());
        }
        
        // Store phone/website in settings JSONB
        if (merchant.getSettings() == null) {
            merchant.setSettings(new java.util.HashMap<>());
        }
        
        if (request.getPhone() != null) {
            merchant.getSettings().put("phone", request.getPhone());
        }
        
        if (request.getWebsite() != null) {
            merchant.getSettings().put("website", request.getWebsite());
        }
        
        merchant = merchantRepository.save(merchant);
        
        log.info("Merchant profile updated: id={}", merchantId);
        auditLogService.log(
            merchantId, "MERCHANT_USER", merchantId.toString(), roleAccessService.currentRole().name(),
            "MERCHANT_PROFILE_UPDATED", "MERCHANT", merchantId.toString(), "SUCCESS",
            Map.of("updatedFields", summarizeProfileFields(request)), httpRequest
        );
        
        return ResponseEntity.ok(buildMerchantResponse(merchant));
    }
    
    @Operation(summary = "Change password")
    @PostMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader,
            HttpServletRequest httpRequest) {
        roleAccessService.requireAny(MerchantRole.ADMIN);
        
        UUID merchantId = merchantIdHeader != null 
            ? UUID.fromString(merchantIdHeader) 
            : null;
        
        if (merchantId == null) {
            return ResponseEntity.status(401).build();
        }
        
        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), merchant.getPasswordHash())) {
            auditLogService.log(
                merchantId, "MERCHANT_USER", merchantId.toString(), roleAccessService.currentRole().name(),
                "MERCHANT_PASSWORD_CHANGED", "MERCHANT", merchantId.toString(), "FAILED",
                Map.of("reason", "invalid_current_password"), httpRequest
            );
            return ResponseEntity.status(400)
                .body(java.util.Map.of("error", "Current password is incorrect"));
        }
        
        // Verify new passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            auditLogService.log(
                merchantId, "MERCHANT_USER", merchantId.toString(), roleAccessService.currentRole().name(),
                "MERCHANT_PASSWORD_CHANGED", "MERCHANT", merchantId.toString(), "FAILED",
                Map.of("reason", "password_mismatch"), httpRequest
            );
            return ResponseEntity.status(400)
                .body(java.util.Map.of("error", "New passwords do not match"));
        }
        
        // Update password
        merchant.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        merchantRepository.save(merchant);
        
        log.info("Password changed for merchant: id={}", merchantId);
        auditLogService.log(
            merchantId, "MERCHANT_USER", merchantId.toString(), roleAccessService.currentRole().name(),
            "MERCHANT_PASSWORD_CHANGED", "MERCHANT", merchantId.toString(), "SUCCESS",
            null, httpRequest
        );
        
        return ResponseEntity.ok(java.util.Map.of("message", "Password changed successfully"));
    }

    @Operation(summary = "Update settlement bank account")
    @PutMapping("/me/bank-account")
    public ResponseEntity<Map<String, String>> updateBankAccount(
            @Valid @RequestBody UpdateBankAccountRequest request,
            @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader,
            HttpServletRequest httpRequest) {
        roleAccessService.requireAny(MerchantRole.ADMIN);

        UUID merchantId = merchantIdHeader != null ? UUID.fromString(merchantIdHeader) : null;
        if (merchantId == null) {
            return ResponseEntity.status(401).build();
        }

        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        ensureSettingsMap(merchant);
        if (!bankAccountCryptoService.isConfigured()) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Payout account updates are temporarily unavailable. Missing encryption configuration."
            );
        }

        Map<String, Object> bankAccount = new HashMap<>();
        bankAccount.put("accountHolderName", request.getAccountHolderName());
        bankAccount.put("accountNumber", bankAccountCryptoService.encrypt(request.getAccountNumber()));
        bankAccount.put("routingNumber", bankAccountCryptoService.encrypt(request.getRoutingNumber()));
        bankAccount.put("accountNumberLast4", last4(request.getAccountNumber()));
        bankAccount.put("routingNumberLast4", last4(request.getRoutingNumber()));
        bankAccount.put("accountType", request.getAccountType().toLowerCase());
        merchant.getSettings().put("bankAccount", bankAccount);

        merchantRepository.save(merchant);
        log.info("Bank account settings updated: merchantId={}", merchantId);
        auditLogService.log(
            merchantId, "MERCHANT_USER", merchantId.toString(), roleAccessService.currentRole().name(),
            "MERCHANT_BANK_ACCOUNT_UPDATED", "MERCHANT", merchantId.toString(), "SUCCESS",
            Map.of(
                "accountType", request.getAccountType().toLowerCase(),
                "accountNumberLast4", last4(request.getAccountNumber()),
                "routingNumberLast4", last4(request.getRoutingNumber())
            ),
            httpRequest
        );
        return ResponseEntity.ok(Map.of("message", "Bank account updated"));
    }

    @Operation(summary = "Update notification preferences")
    @PutMapping("/me/notifications")
    public ResponseEntity<Map<String, String>> updateNotifications(
            @Valid @RequestBody UpdateNotificationPreferencesRequest request,
            @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader,
            HttpServletRequest httpRequest) {
        roleAccessService.requireAny(MerchantRole.ADMIN);

        UUID merchantId = merchantIdHeader != null ? UUID.fromString(merchantIdHeader) : null;
        if (merchantId == null) {
            return ResponseEntity.status(401).build();
        }

        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        ensureSettingsMap(merchant);
        Map<String, Object> notifications = new HashMap<>();
        notifications.put("emailOnPayment", request.isEmailOnPayment());
        notifications.put("emailOnRefund", request.isEmailOnRefund());
        notifications.put("emailOnPayout", request.isEmailOnPayout());
        notifications.put("emailOnFraud", request.isEmailOnFraud());
        merchant.getSettings().put("notifications", notifications);

        merchantRepository.save(merchant);
        log.info("Notification settings updated: merchantId={}", merchantId);
        auditLogService.log(
            merchantId, "MERCHANT_USER", merchantId.toString(), roleAccessService.currentRole().name(),
            "MERCHANT_NOTIFICATIONS_UPDATED", "MERCHANT", merchantId.toString(), "SUCCESS",
            Map.of(
                "emailOnPayment", request.isEmailOnPayment(),
                "emailOnRefund", request.isEmailOnRefund(),
                "emailOnPayout", request.isEmailOnPayout(),
                "emailOnFraud", request.isEmailOnFraud()
            ),
            httpRequest
        );
        return ResponseEntity.ok(Map.of("message", "Notification preferences updated"));
    }
    
    @Operation(summary = "Delete merchant account")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
            @RequestHeader(value = "X-Merchant-ID", required = false) String merchantIdHeader,
            HttpServletRequest httpRequest) {
        roleAccessService.requireAny(MerchantRole.ADMIN);
        
        UUID merchantId = merchantIdHeader != null 
            ? UUID.fromString(merchantIdHeader) 
            : null;
        
        if (merchantId == null) {
            return ResponseEntity.status(401).build();
        }
        
        Merchant merchant = merchantRepository.findById(merchantId)
            .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        
        // Soft delete - set status to CLOSED
        merchant.setStatus("CLOSED");
        merchantRepository.save(merchant);
        
        log.warn("Merchant account deleted: id={}", merchantId);
        auditLogService.log(
            merchantId, "MERCHANT_USER", merchantId.toString(), roleAccessService.currentRole().name(),
            "MERCHANT_ACCOUNT_DELETED", "MERCHANT", merchantId.toString(), "SUCCESS",
            Map.of("mode", "soft_delete", "newStatus", "CLOSED"),
            httpRequest
        );
        
        return ResponseEntity.noContent().build();
    }

    private void ensureSettingsMap(Merchant merchant) {
        if (merchant.getSettings() == null) {
            merchant.setSettings(new HashMap<>());
        }
    }

    private MerchantResponse buildMerchantResponse(Merchant merchant) {
        Map<String, Object> maskedBankAccount = maskBankAccount(getSettingsMap(merchant, "bankAccount"));
        return MerchantResponse.builder()
            .id(merchant.getId().toString())
            .businessName(merchant.getBusinessName())
            .email(merchant.getEmail())
            .phone(getSettingsString(merchant, "phone"))
            .website(getSettingsString(merchant, "website"))
            .bankAccount(maskedBankAccount)
            .notifications(getSettingsMap(merchant, "notifications"))
            .status(merchant.getStatus())
            .riskProfile(merchant.getRiskProfile())
            .role(merchant.getRole())
            .settings(sanitizeSettings(merchant.getSettings(), maskedBankAccount))
            .createdAt(merchant.getCreatedAt())
            .build();
    }

    private Map<String, Object> sanitizeSettings(Map<String, Object> settings, Map<String, Object> maskedBankAccount) {
        if (settings == null) {
            return null;
        }
        Map<String, Object> sanitized = new HashMap<>(settings);
        if (sanitized.containsKey("bankAccount")) {
            sanitized.put("bankAccount", maskedBankAccount);
        }
        return sanitized;
    }

    private Map<String, Object> maskBankAccount(Map<String, Object> bankAccount) {
        if (bankAccount == null) {
            return null;
        }
        Map<String, Object> masked = new HashMap<>(bankAccount);
        String accountNumber = valueAsString(masked.get("accountNumber"));
        String routingNumber = valueAsString(masked.get("routingNumber"));
        String accountNumberLast4 = valueAsString(masked.get("accountNumberLast4"));
        String routingNumberLast4 = valueAsString(masked.get("routingNumberLast4"));

        if ((accountNumberLast4 == null || accountNumberLast4.isBlank()) && accountNumber != null && !accountNumber.isBlank()) {
            String plaintextOrDecrypted = bankAccountCryptoService.isEncrypted(accountNumber)
                ? bankAccountCryptoService.decryptIfEncrypted(accountNumber)
                : accountNumber;
            accountNumberLast4 = last4(plaintextOrDecrypted);
            masked.put("accountNumberLast4", accountNumberLast4);
        }
        if ((routingNumberLast4 == null || routingNumberLast4.isBlank()) && routingNumber != null && !routingNumber.isBlank()) {
            String plaintextOrDecrypted = bankAccountCryptoService.isEncrypted(routingNumber)
                ? bankAccountCryptoService.decryptIfEncrypted(routingNumber)
                : routingNumber;
            routingNumberLast4 = last4(plaintextOrDecrypted);
            masked.put("routingNumberLast4", routingNumberLast4);
        }

        if (accountNumberLast4 != null && !accountNumberLast4.isBlank()) {
            masked.put("accountNumber", "••••" + accountNumberLast4);
        } else if (accountNumber != null && !accountNumber.isBlank()) {
            masked.put("accountNumber", maskSensitive(accountNumber, 4));
        }
        if (routingNumberLast4 != null && !routingNumberLast4.isBlank()) {
            masked.put("routingNumber", "•••••" + routingNumberLast4);
        } else if (routingNumber != null && !routingNumber.isBlank()) {
            masked.put("routingNumber", maskSensitive(routingNumber, 4));
        }
        return masked;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getSettingsMap(Merchant merchant, String key) {
        if (merchant.getSettings() == null) {
            return null;
        }
        Object value = merchant.getSettings().get(key);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private String getSettingsString(Merchant merchant, String key) {
        if (merchant.getSettings() == null) {
            return null;
        }
        Object value = merchant.getSettings().get(key);
        return value != null ? value.toString() : null;
    }

    private String valueAsString(Object value) {
        return value != null ? value.toString() : null;
    }

    private String last4(String value) {
        if (value.length() <= 4) {
            return value;
        }
        return value.substring(value.length() - 4);
    }

    private String maskSensitive(String value, int visibleSuffix) {
        if (value.length() <= visibleSuffix) {
            return "•".repeat(value.length());
        }
        int maskedLength = value.length() - visibleSuffix;
        return "•".repeat(maskedLength) + value.substring(maskedLength);
    }

    private java.util.List<String> summarizeProfileFields(UpdateProfileRequest request) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        if (request.getBusinessName() != null) fields.add("businessName");
        if (request.getPhone() != null) fields.add("phone");
        if (request.getWebsite() != null) fields.add("website");
        return fields;
    }
}
