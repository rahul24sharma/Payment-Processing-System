package com.payment.fraud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "velocity_counters", indexes = {
    @Index(name = "idx_velocity_window", columnList = "window_end"),
    @Index(name = "idx_velocity_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VelocityCounter {
    
    @Id
    private String id; // e.g., "card:4242:1h", "ip:192.168.1.1:24h"
    
    @Column(nullable = false)
    @Builder.Default
    private Integer counter = 0;
    
    @Column(name = "window_start", nullable = false)
    private Instant windowStart;
    
    @Column(name = "window_end", nullable = false)
    private Instant windowEnd;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    public void increment() {
        this.counter++;
    }
    
    public boolean isExpired() {
        return Instant.now().isAfter(windowEnd);
    }
}