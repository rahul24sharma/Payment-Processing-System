package com.payment.ledger.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class ReconciliationReportResponse {
    Instant start;
    Instant end;
    Integer totalEntries;
    Integer totalEntryGroups;
    BigDecimal totalDebits;
    BigDecimal totalCredits;
    Boolean balanced;
    BigDecimal difference;
}
