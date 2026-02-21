package com.payment.service.entity;

public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    VOID,
    REFUNDED,
    PARTIALLY_REFUNDED,
    FAILED,
    DECLINED,
    EXPIRED
}