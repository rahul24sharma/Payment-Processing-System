package com.payment.settlement.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Value
@Builder
public class SettlementReportResponse {
    LocalDate startDate;
    LocalDate endDate;
    Integer batchCount;
    Integer payoutCount;
    Integer totalPaymentCount;
    BigDecimal totalGrossAmount;
    BigDecimal totalFeeAmount;
    BigDecimal totalNetAmount;
    BigDecimal totalReserveAmount;
    BigDecimal totalPayoutAmount;
    Map<String, Long> payoutStatusCounts;
    Map<String, Long> batchStatusCounts;
}
