package com.payment.service.exception;

import java.util.ArrayList;
import java.util.List;

public class ValidationException extends PaymentException {
    
    private final List<ValidationError> errors;
    
    public ValidationException(String message) {
        super(message, "validation_error");
        this.errors = new ArrayList<>();
    }
    
    public ValidationException(List<ValidationError> errors) {
        super("Validation failed", "validation_error");
        this.errors = errors;
        addDetail("validation_errors", errors);
    }
    
    public List<ValidationError> getErrors() {
        return errors;
    }
    
    public static class ValidationError {
        private String field;
        private String message;
        private Object rejectedValue;
        
        public ValidationError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }
        
        // Getters
        public String getField() { return field; }
        public String getMessage() { return message; }
        public Object getRejectedValue() { return rejectedValue; }
    }
}