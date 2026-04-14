package com.oscargsedas.transactionsystem.repository;

import com.oscargsedas.transactionsystem.entity.LedgerLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LedgerLineRepository extends JpaRepository<LedgerLine, UUID> {
}