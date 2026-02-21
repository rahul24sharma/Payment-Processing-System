package com.payment.fraud.exception;

import com.payment.fraud.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(FraudException.class)
    public ResponseEntity<ErrorResponse> handleFraudException(FraudException ex) {
        log.error("Fraud exception occurred: code={}, message={}", 
            ex.getErrorCode(), ex.getMessage(), ex);
        
        ErrorResponse response = ErrorResponse.builder()
            .error(ErrorResponse.ErrorDetail.builder()
                .type("fraud_error")
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .details(ex.getDetails())
                .timestamp(Instant.now())
                .build())
            .build();
        
        HttpStatus status = determineHttpStatus(ex);
        return ResponseEntity.status(status).body(response);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        log.warn("Validation error occurred");
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        Map<String, Object> details = new HashMap<>();
        details.put("errors", errors);
        
        ErrorResponse response = ErrorResponse.builder()
            .error(ErrorResponse.ErrorDetail.builder()
                .type("validation_error")
                .code("invalid_request")
                .message("Request validation failed")
                .details(details)
                .timestamp(Instant.now())
                .build())
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseException(DataAccessException ex) {
        log.error("Database error occurred", ex);
        
        ErrorResponse response = ErrorResponse.builder()
            .error(ErrorResponse.ErrorDetail.builder()
                .type("internal_error")
                .code("database_error")
                .message("A database error occurred. Please try again later.")
                .timestamp(Instant.now())
                .build())
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        
        ErrorResponse response = ErrorResponse.builder()
            .error(ErrorResponse.ErrorDetail.builder()
                .type("internal_error")
                .code("internal_server_error")
                .message("An unexpected error occurred. Please contact support.")
                .timestamp(Instant.now())
                .build())
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    private HttpStatus determineHttpStatus(FraudException ex) {
        return switch (ex.getErrorCode()) {
            case "rule_not_found" -> HttpStatus.NOT_FOUND;
            case "validation_error" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}