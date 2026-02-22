package com.payment.ledger.repository;

import com.payment.ledger.entity.AccountBalance;
import com.payment.ledger.entity.AccountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, UUID> {
    
    Optional<AccountBalance> findByAccountId(UUID accountId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountBalance a WHERE a.accountId = :accountId")
    Optional<AccountBalance> findByAccountIdForUpdate(@Param("accountId") UUID accountId);
    
    List<AccountBalance> findByAccountType(AccountType accountType);
    
    @Query("SELECT a FROM AccountBalance a WHERE a.accountType = :type AND a.currency = :currency")
    List<AccountBalance> findByAccountTypeAndCurrency(
        @Param("type") AccountType type,
        @Param("currency") String currency
    );
}