package com.payment.fraud.exception;

public class FraudServiceException extends FraudException {
    
    public FraudServiceException(String message) {
        super(message, "fraud_service_error");
    }
}