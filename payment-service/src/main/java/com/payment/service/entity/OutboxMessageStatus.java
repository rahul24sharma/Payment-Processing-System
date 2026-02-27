package com.payment.service.entity;

public enum OutboxMessageStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
