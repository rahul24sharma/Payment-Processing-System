package com.payment.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationPreferencesRequest {
    private boolean emailOnPayment;
    private boolean emailOnRefund;
    private boolean emailOnPayout;
    private boolean emailOnFraud;
}
