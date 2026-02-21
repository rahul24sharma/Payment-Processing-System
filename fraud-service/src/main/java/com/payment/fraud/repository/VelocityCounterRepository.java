package com.payment.fraud.repository;

import com.payment.fraud.entity.VelocityCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface VelocityCounterRepository extends JpaRepository<VelocityCounter, String> {
    
    @Query("SELECT v FROM VelocityCounter v WHERE v.windowEnd < :now")
    List<VelocityCounter> findExpiredCounters(@Param("now") Instant now);
    
    @Modifying
    @Query("DELETE FROM VelocityCounter v WHERE v.windowEnd < :expiryDate")
    int deleteExpiredCounters(@Param("expiryDate") Instant expiryDate);
}