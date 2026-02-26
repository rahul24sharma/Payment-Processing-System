package com.payment.settlement.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
public class ReserveReportResponse {
    LocalDate startDate;
    LocalDate endDate;
    UUID merchantId;
    Integer payoutCount;
    BigDecimal totalReserveHeld;
    BigDecimal totalPayoutAmount;
    BigDecimal totalNetAmount;
    BigDecimal reserveRatePercentOfNet;
}
