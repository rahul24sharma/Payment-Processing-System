package com.payment.service.exception;

import com.payment.service.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Handles all PaymentException subclasses
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(PaymentException ex) {
        log.error("Payment exception occurred: code={}, message={}", 
            ex.getErrorCode(), ex.getMessage(), ex);
        
        ErrorResponse response = ErrorResponse.builder()
            .error(ErrorResponse.ErrorDetail.builder()
                .type("payment_error")
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .details(ex.getDetails())
                .timestamp(Instant.now())
                .build())
            .build();
        
        HttpStatus status = determineHttpStatus(ex);
        
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * Handles validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        log.warn("Validation error occurred: {}", ex.getMessage());
        
        List<ValidationException.ValidationError> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ValidationException.ValidationError(
                error.getField(),
                error.getDefaultMessage(),
                error.getRejectedValue()
            ))
            .collect(Collectors.toList());
        
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

    /**
     * Handles validation errors from method parameters (@RequestParam, @PathVariable, etc.)
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex) {

        log.warn("Handler method validation error occurred: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
            .error(ErrorResponse.ErrorDetail.builder()
                .type("validation_error")
                .code("invalid_request")
                .message("Request validation failed")
                .timestamp(Instant.now())
                .build())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handles optimistic locking failures (concurrent updates)
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(
            OptimisticLockingFailureException ex) {
        
        log.warn("Optimistic locking failure: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
            .error(ErrorResponse.ErrorDetail.builder()
                .type("conflict_error")
                .code("concurrent_modification")
                .message("Resource was modified by another request. Please retry.")
                .timestamp(Instant.now())
                .build())
            .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    /**
     * Handles database errors
     */
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
    
    /**
     * Handles 404s for unknown routes
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundException(Exception ex) {
        log.warn("No handler found: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
            .error(ErrorResponse.ErrorDetail.builder()
                .type("not_found")
                .code("endpoint_not_found")
                .message("The requested endpoint does not exist.")
                .timestamp(Instant.now())
                .build())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles all other unexpected exceptions
     */
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
    
    /**
     * Determines appropriate HTTP status code based on exception type
     */
    private HttpStatus determineHttpStatus(PaymentException ex) {
        return switch (ex.getErrorCode()) {
            case "payment_not_found" -> HttpStatus.NOT_FOUND;
            case "invalid_state_transition", "concurrent_modification" -> HttpStatus.CONFLICT;
            case "invalid_amount", "validation_error" -> HttpStatus.BAD_REQUEST;
            case "processor_error", "insufficient_funds" -> HttpStatus.PAYMENT_REQUIRED;
            case "rate_limit_exceeded" -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
