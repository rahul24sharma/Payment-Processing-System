package com.payment.fraud.service;

import com.payment.fraud.dto.FraudAssessmentRequest;
import com.payment.fraud.entity.Blocklist;
import com.payment.fraud.repository.BlocklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class BlocklistService {
    
    private final BlocklistRepository blocklistRepository;
    
    /**
     * Check if any entity in the request is blocklisted
     */
    public boolean isBlocked(FraudAssessmentRequest request) {
        // Check customer email (if available via metadata)
        if (request.getMetadata() != null && request.getMetadata().containsKey("email")) {
            String email = (String) request.getMetadata().get("email");
            if (isBlocklisted("EMAIL", email)) {
                log.warn("Blocked email detected: {}", email);
                return true;
            }
        }
        
        // Check IP address
        if (request.getIpAddress() != null && isBlocklisted("IP", request.getIpAddress())) {
            log.warn("Blocked IP detected: {}", request.getIpAddress());
            return true;
        }
        
        // Check device ID
        if (request.getDeviceId() != null && isBlocklisted("DEVICE_ID", request.getDeviceId())) {
            log.warn("Blocked device detected: {}", request.getDeviceId());
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if specific value is blocklisted
     */
    private boolean isBlocklisted(String type, String value) {
        return blocklistRepository.findByTypeAndValueAndIsActiveTrue(type, value)
            .map(blocklist -> !blocklist.isExpired())
            .orElse(false);
    }
    
    /**
     * Add to blocklist
     */
    @Transactional
    public Blocklist addToBlocklist(String type, String value, String reason) {
        log.info("Adding to blocklist: type={}, value={}", type, value);
        
        Blocklist blocklist = Blocklist.builder()
            .type(type)
            .value(value)
            .reason(reason)
            .isActive(true)
            .createdBy("SYSTEM")
            .build();
        
        return blocklistRepository.save(blocklist);
    }
    
    /**
     * Remove from blocklist
     */
    @Transactional
    public void removeFromBlocklist(UUID id) {
        blocklistRepository.findById(id).ifPresent(blocklist -> {
            blocklist.setIsActive(false);
            blocklistRepository.save(blocklist);
            log.info("Removed from blocklist: id={}", id);
        });
    }
}