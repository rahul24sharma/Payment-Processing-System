package com.payment.service.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all payment-related errors
 */
public abstract class PaymentException extends RuntimeException {
    
    private final String errorCode;
    private final Map<String, Object> details;
    
    protected PaymentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }
    
    protected PaymentException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = new HashMap<>();
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
    
    public void addDetail(String key, Object value) {
        this.details.put(key, value);
    }
}