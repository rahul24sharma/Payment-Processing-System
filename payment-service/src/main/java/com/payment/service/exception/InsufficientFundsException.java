package com.payment.service.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends PaymentException {
    
    public InsufficientFundsException(BigDecimal available, BigDecimal requested) {
        super(
            String.format("Insufficient funds: available=%s, requested=%s", available, requested),
            "insufficient_funds"
        );
        addDetail("available_amount", available);
        addDetail("requested_amount", requested);
    }
}