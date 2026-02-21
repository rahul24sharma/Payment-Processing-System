package com.payment.service.exception;

public class InvalidAmountException extends PaymentException {
    
    public InvalidAmountException(String message) {
        super(message, "invalid_amount");
    }
}