package com.payment.ledger.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class LedgerEntryGroupResponse {
    UUID entryGroupId;
    String currency;
    UUID paymentId;
    UUID refundId;
    UUID settlementId;
    String description;
    BigDecimal totalDebits;
    BigDecimal totalCredits;
    Integer entryCount;
    List<Long> ledgerEntryIds;
}
