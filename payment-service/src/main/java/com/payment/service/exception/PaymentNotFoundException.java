package com.payment.service.exception;

import java.util.UUID;

public class PaymentNotFoundException extends PaymentException {
    
    public PaymentNotFoundException(UUID paymentId) {
        super(
            String.format("Payment not found: %s", paymentId),
            "payment_not_found"
        );
        addDetail("payment_id", paymentId.toString());
    }
    
    public PaymentNotFoundException(String paymentId) {
        super(
            String.format("Payment not found: %s", paymentId),
            "payment_not_found"
        );
        addDetail("payment_id", paymentId);
    }
}