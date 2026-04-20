package com.oscargsedas.transactionsystem.repository;

import com.oscargsedas.transactionsystem.entity.LedgerLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface LedgerLineRepository extends JpaRepository<LedgerLine, UUID> {
	@Query("select coalesce(sum(l.amount), 0) from LedgerLine l where l.account.id = :accountId")
	BigDecimal getAccountBalance(@Param("accountId") UUID accountId);
}