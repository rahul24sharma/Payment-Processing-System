package com.payment.notification.scheduler;

import com.payment.notification.entity.Webhook;
import com.payment.notification.repository.WebhookRepository;
import com.payment.notification.service.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class WebhookRetryScheduler {
    
    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryService webhookDeliveryService;
    
    /**
     * Retry failed webhooks every minute
     */
    @Scheduled(fixedDelay = 60000) // Every 1 minute
    public void retryFailedWebhooks() {
        List<Webhook> webhooksToRetry = webhookRepository
            .findPendingWebhooksForRetry(Instant.now());
        
        if (webhooksToRetry.isEmpty()) {
            return;
        }
        
        log.info("Retrying {} failed webhooks", webhooksToRetry.size());
        
        for (Webhook webhook : webhooksToRetry) {
            try {
                webhookDeliveryService.deliverWebhook(webhook);
            } catch (Exception e) {
                log.error("Error retrying webhook: id={}", webhook.getId(), e);
            }
        }
    }
}