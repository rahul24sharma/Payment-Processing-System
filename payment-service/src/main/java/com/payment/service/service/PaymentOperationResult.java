package com.payment.service.service;

import com.payment.service.dto.response.PaymentNextActionResponse;
import com.payment.service.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOperationResult {
    private Payment payment;
    private PaymentNextActionResponse nextAction;

    public static PaymentOperationResult of(Payment payment) {
        return PaymentOperationResult.builder().payment(payment).build();
    }

    public static PaymentOperationResult withNextAction(Payment payment, PaymentNextActionResponse nextAction) {
        return PaymentOperationResult.builder().payment(payment).nextAction(nextAction).build();
    }
}
