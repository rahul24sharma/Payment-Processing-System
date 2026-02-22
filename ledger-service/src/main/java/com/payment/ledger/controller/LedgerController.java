package com.payment.ledger.controller;

import com.payment.ledger.entity.LedgerEntry;
import com.payment.ledger.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ledger")
@Tag(name = "Ledger", description = "Ledger and accounting endpoints")
@RequiredArgsConstructor
@Slf4j
public class LedgerController {
    
    private final LedgerService ledgerService;
    
    @Operation(summary = "Get account balance")
    @GetMapping("/balance/{accountId}")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable UUID accountId) {
        BigDecimal balance = ledgerService.getBalance(accountId);
        
        return ResponseEntity.ok(Map.of(
            "accountId", accountId.toString(),
            "balance", balance,
            "timestamp", java.time.Instant.now()
        ));
    }
    
    @Operation(summary = "Get account ledger entries")
    @GetMapping("/entries/{accountId}")
    public ResponseEntity<List<LedgerEntry>> getEntries(@PathVariable UUID accountId) {
        List<LedgerEntry> entries = ledgerService.getAccountEntries(accountId);
        return ResponseEntity.ok(entries);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Ledger Service is UP");
    }
}