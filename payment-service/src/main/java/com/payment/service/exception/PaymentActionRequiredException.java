package com.payment.service.exception;

public class PaymentActionRequiredException extends ProcessorException {

    private final String paymentIntentId;
    private final String clientSecret;
    private final String nextActionType;

    public PaymentActionRequiredException(
            String message,
            String paymentIntentId,
            String clientSecret,
            String nextActionType
    ) {
        super(message, "stripe", "requires_action");
        this.paymentIntentId = paymentIntentId;
        this.clientSecret = clientSecret;
        this.nextActionType = nextActionType;
        addDetail("payment_intent_id", paymentIntentId);
        addDetail("client_secret", clientSecret);
        addDetail("next_action_type", nextActionType);
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getNextActionType() {
        return nextActionType;
    }
}
