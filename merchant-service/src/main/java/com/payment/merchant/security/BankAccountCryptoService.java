package com.payment.merchant.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class BankAccountCryptoService {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String LEGACY_PREFIX = "enc:v1:";
    private static final String VERSIONED_PREFIX = "enc:v2:";
    private static final String DEFAULT_LEGACY_KEY_ID = "v1";

    private final String legacyConfiguredKey;
    private final String configuredActiveKeyId;
    private final String configuredKeyring;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, byte[]> keyring;
    private final String activeKeyId;

    public BankAccountCryptoService(
        @Value("${merchant.security.bank-account-encryption-key:}") String legacyConfiguredKey,
        @Value("${merchant.security.bank-account-encryption.active-key-id:}") String configuredActiveKeyId,
        @Value("${merchant.security.bank-account-encryption.keys:}") String configuredKeyring
    ) {
        this.legacyConfiguredKey = legacyConfiguredKey != null ? legacyConfiguredKey.trim() : "";
        this.configuredActiveKeyId = configuredActiveKeyId != null ? configuredActiveKeyId.trim() : "";
        this.configuredKeyring = configuredKeyring != null ? configuredKeyring.trim() : "";
        this.keyring = parseKeyring(this.configuredKeyring, this.legacyConfiguredKey);
        this.activeKeyId = resolveActiveKeyId(this.keyring, this.configuredActiveKeyId);
    }

    public boolean isConfigured() {
        return activeKeyId != null && keyring.containsKey(activeKeyId);
    }

    public String getActiveKeyId() {
        return activeKeyId;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        ensureConfigured();
        try {
            byte[] keyBytes = keyring.get(activeKeyId);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, iv)
            );

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return VERSIONED_PREFIX
                + activeKeyId
                + ":"
                + Base64.getEncoder().encodeToString(iv)
                + ":"
                + Base64.getEncoder().encodeToString(ciphertext);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to encrypt bank account field", e);
            throw new IllegalStateException("Failed to encrypt bank account field", e);
        }
    }

    public String decryptIfEncrypted(String value) {
        if (!isEncrypted(value)) {
            return value;
        }
        ensureConfigured();
        try {
            ParsedCiphertext parsed = parseCiphertext(value);
            byte[] keyBytes = keyring.get(parsed.keyId());
            if (keyBytes == null) {
                throw new IllegalStateException("No bank account encryption key configured for key id: " + parsed.keyId());
            }

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new GCMParameterSpec(GCM_TAG_BITS, parsed.iv())
            );
            byte[] plaintext = cipher.doFinal(parsed.ciphertext());
            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to decrypt bank account field", e);
            throw new IllegalStateException("Failed to decrypt bank account field", e);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && (value.startsWith(LEGACY_PREFIX) || value.startsWith(VERSIONED_PREFIX));
    }

    public boolean requiresReencryption(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (!isEncrypted(value)) {
            return true;
        }
        if (value.startsWith(LEGACY_PREFIX)) {
            return true;
        }
        ParsedCiphertext parsed = parseCiphertext(value);
        return activeKeyId != null && !activeKeyId.equals(parsed.keyId());
    }

    public String reencryptToActive(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String plaintext = decryptIfEncrypted(value);
        return encrypt(plaintext);
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("Bank account encryption key is not configured");
        }
    }

    private Map<String, byte[]> parseKeyring(String rawKeyring, String legacyKey) {
        Map<String, byte[]> parsed = new HashMap<>();

        if (!rawKeyring.isBlank()) {
            String[] entries = rawKeyring.split(",");
            for (String entry : entries) {
                String trimmed = entry.trim();
                if (trimmed.isBlank()) {
                    continue;
                }
                String[] parts = trimmed.split(":", 2);
                if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                    throw new IllegalStateException(
                        "MERCHANT_BANK_ACCOUNT_ENCRYPTION_KEYS must use 'keyId:base64Key' comma-separated format"
                    );
                }
                parsed.put(parts[0].trim(), decodeKey(parts[1].trim(), "MERCHANT_BANK_ACCOUNT_ENCRYPTION_KEYS[" + parts[0].trim() + "]"));
            }
        }

        if (!legacyKey.isBlank()) {
            parsed.putIfAbsent(DEFAULT_LEGACY_KEY_ID, decodeKey(legacyKey, "MERCHANT_BANK_ACCOUNT_ENCRYPTION_KEY"));
        }

        return parsed;
    }

    private String resolveActiveKeyId(Map<String, byte[]> parsedKeyring, String configuredActiveId) {
        if (parsedKeyring.isEmpty()) {
            return null;
        }
        if (!configuredActiveId.isBlank()) {
            if (!parsedKeyring.containsKey(configuredActiveId)) {
                throw new IllegalStateException(
                    "MERCHANT_BANK_ACCOUNT_ENCRYPTION_ACTIVE_KEY_ID is not present in configured keyring: " + configuredActiveId
                );
            }
            return configuredActiveId;
        }
        if (parsedKeyring.containsKey(DEFAULT_LEGACY_KEY_ID)) {
            return DEFAULT_LEGACY_KEY_ID;
        }
        return parsedKeyring.keySet().iterator().next();
    }

    private byte[] decodeKey(String base64Key, String sourceName) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            if (keyBytes.length != 32) {
                throw new IllegalStateException(sourceName + " must be base64-encoded 32 bytes");
            }
            return keyBytes;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(sourceName + " must be valid base64", e);
        }
    }

    private ParsedCiphertext parseCiphertext(String value) {
        if (value.startsWith(VERSIONED_PREFIX)) {
            String payload = value.substring(VERSIONED_PREFIX.length());
            String[] parts = payload.split(":", 3);
            if (parts.length != 3) {
                throw new IllegalStateException("Invalid versioned encrypted bank account payload format");
            }
            return new ParsedCiphertext(
                parts[0],
                Base64.getDecoder().decode(parts[1]),
                Base64.getDecoder().decode(parts[2])
            );
        }

        if (value.startsWith(LEGACY_PREFIX)) {
            String payload = value.substring(LEGACY_PREFIX.length());
            String[] parts = payload.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalStateException("Invalid legacy encrypted bank account payload format");
            }
            return new ParsedCiphertext(
                DEFAULT_LEGACY_KEY_ID,
                Base64.getDecoder().decode(parts[0]),
                Base64.getDecoder().decode(parts[1])
            );
        }

        throw new IllegalStateException("Unsupported encrypted bank account payload format");
    }

    private record ParsedCiphertext(String keyId, byte[] iv, byte[] ciphertext) {}
}
