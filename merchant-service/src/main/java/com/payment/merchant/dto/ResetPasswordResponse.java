package com.payment.merchant.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResetPasswordResponse {

    private String message;
}
