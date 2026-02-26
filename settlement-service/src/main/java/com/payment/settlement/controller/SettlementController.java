package com.payment.settlement.controller;

import com.payment.settlement.entity.Payout;
import com.payment.settlement.entity.SettlementBatch;
import com.payment.settlement.dto.ReserveReportResponse;
import com.payment.settlement.dto.SettlementReportResponse;
import com.payment.settlement.scheduler.SettlementScheduler;
import com.payment.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settlements")
@Tag(name = "Settlement", description = "Settlement and payout endpoints")
@RequiredArgsConstructor
@Slf4j
public class SettlementController {
    
    private final SettlementService settlementService;
    private final SettlementScheduler settlementScheduler;
    
    @Operation(summary = "Get settlement history")
    @GetMapping
    public ResponseEntity<List<SettlementBatch>> getSettlementHistory() {
        List<SettlementBatch> history = settlementService.getSettlementHistory(10);
        return ResponseEntity.ok(history);
    }
    
    @Operation(summary = "Get payouts for a merchant")
    @GetMapping("/payouts/merchant/{merchantId}")
    public ResponseEntity<List<Payout>> getMerchantPayouts(@PathVariable UUID merchantId) {
        List<Payout> payouts = settlementService.getMerchantPayouts(merchantId);
        return ResponseEntity.ok(payouts);
    }

    @Operation(summary = "Get settlement report for a date range")
    @GetMapping("/reports")
    public ResponseEntity<SettlementReportResponse> getSettlementReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(settlementService.getSettlementReport(startDate, endDate));
    }

    @Operation(summary = "Get reserve report for a date range (optionally filtered by merchant)")
    @GetMapping("/reserves")
    public ResponseEntity<ReserveReportResponse> getReserveReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID merchantId) {
        return ResponseEntity.ok(settlementService.getReserveReport(startDate, endDate, merchantId));
    }
    
    @Operation(summary = "Trigger manual settlement (admin only)")
    @PostMapping("/trigger")
    public ResponseEntity<SettlementBatch> triggerSettlement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        log.info("Manual settlement triggered for date: {}", date);
        
        SettlementBatch batch = settlementScheduler.triggerManualSettlement(date);
        
        return ResponseEntity.ok(batch);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Settlement Service is UP");
    }
}
