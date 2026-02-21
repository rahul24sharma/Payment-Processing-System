package com.payment.service.exception;

public class InvalidStateTransitionException extends PaymentException {
    
    public InvalidStateTransitionException(String fromStatus, String toStatus) {
        super(
            String.format("Cannot transition from %s to %s", fromStatus, toStatus),
            "invalid_state_transition"
        );
        addDetail("from_status", fromStatus);
        addDetail("to_status", toStatus);
    }
}