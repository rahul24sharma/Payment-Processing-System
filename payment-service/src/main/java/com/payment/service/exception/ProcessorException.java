package com.payment.service.exception;

public class ProcessorException extends PaymentException {
    
    private final String processorName;
    private final String processorErrorCode;
    
    public ProcessorException(String message, String processorName, String processorErrorCode) {
        super(message, "processor_error");
        this.processorName = processorName;
        this.processorErrorCode = processorErrorCode;
        addDetail("processor_name", processorName);
        addDetail("processor_error_code", processorErrorCode);
    }
    
    public ProcessorException(String message, String processorName, 
                             String processorErrorCode, Throwable cause) {
        super(message, "processor_error", cause);
        this.processorName = processorName;
        this.processorErrorCode = processorErrorCode;
        addDetail("processor_name", processorName);
        addDetail("processor_error_code", processorErrorCode);
    }
}