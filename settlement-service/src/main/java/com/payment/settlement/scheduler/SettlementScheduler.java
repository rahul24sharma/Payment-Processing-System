package com.payment.settlement.scheduler;

import com.payment.settlement.entity.SettlementBatch;
import com.payment.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class SettlementScheduler {
    
    private final SettlementService settlementService;
    
    /**
     * Run daily settlement at 2 AM UTC
     * Cron: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    public void runDailySettlement() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        
        log.info("============================================");
        log.info("Starting daily settlement job: {}", today);
        log.info("============================================");
        
        try {
            SettlementBatch batch = settlementService.processSettlement(today);
            
            log.info("============================================");
            log.info("Settlement completed: batchId={}, status={}, payouts={}", 
                batch.getId(), batch.getStatus(), batch.getTotalPayouts());
            log.info("============================================");
            
        } catch (Exception e) {
            log.error("============================================");
            log.error("SETTLEMENT FAILED for date: {}", today, e);
            log.error("============================================");
            
            // TODO: Alert ops team via PagerDuty/Slack
        }
    }
    
    /**
     * Manual settlement trigger (for testing or re-runs)
     */
    public SettlementBatch triggerManualSettlement(LocalDate date) {
        log.info("Triggering manual settlement for date: {}", date);
        return settlementService.processSettlement(date);
    }
}