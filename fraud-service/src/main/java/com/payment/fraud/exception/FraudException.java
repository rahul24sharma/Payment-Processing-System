package com.payment.fraud.exception;

import java.util.HashMap;
import java.util.Map;

public abstract class FraudException extends RuntimeException {
    
    private final String errorCode;
    private final Map<String, Object> details;
    
    protected FraudException(String message, String errorCode) {
        super(message);
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