package com.payment.merchant.security;

public enum MerchantRole {
    ADMIN,
    SUPPORT,
    DEVELOPER,
    VIEWER;

    public static MerchantRole from(String value) {
        if (value == null || value.isBlank()) {
            return ADMIN;
        }
        return MerchantRole.valueOf(value.trim().toUpperCase());
    }
}
