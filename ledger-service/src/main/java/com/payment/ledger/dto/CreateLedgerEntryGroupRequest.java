package com.payment.ledger.dto;

import com.payment.ledger.entity.AccountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateLedgerEntryGroupRequest {

    @NotBlank
    private String currency;

    private UUID paymentId;
    private UUID refundId;
    private UUID settlementId;

    @NotBlank
    private String description;

    @NotEmpty
    @Valid
    private List<EntryLine> entries;

    @Data
    public static class EntryLine {
        @NotNull
        private UUID accountId;

        @NotNull
        private AccountType accountType;

        private BigDecimal debitAmount;
        private BigDecimal creditAmount;

        private String description;
    }
}
