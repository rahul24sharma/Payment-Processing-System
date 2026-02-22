package com.payment.ledger.repository;

import com.payment.ledger.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    
    /**
     * Find all entries for an entry group (for validation)
     */
    List<LedgerEntry> findByEntryGroupId(UUID entryGroupId);
    
    /**
     * Find all entries for an account
     */
    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(UUID accountId);
    
    /**
     * Find all entries for a payment
     */
    List<LedgerEntry> findByPaymentId(UUID paymentId);
    
    /**
     * Find entries created between dates
     */
    @Query("""
        SELECT e FROM LedgerEntry e 
        WHERE e.createdAt BETWEEN :start AND :end
        ORDER BY e.createdAt DESC
        """)
    List<LedgerEntry> findEntriesBetween(
        @Param("start") Instant start,
        @Param("end") Instant end
    );
    
    /**
     * Get all unique entry group IDs for a date range
     */
    @Query("SELECT DISTINCT e.entryGroupId FROM LedgerEntry e WHERE e.createdAt BETWEEN :start AND :end")
    List<UUID> findEntryGroupsByDateRange(
        @Param("start") Instant start,
        @Param("end") Instant end
    );
}