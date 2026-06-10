package com.oscargsedas.transactionsystem.repository;

import com.oscargsedas.transactionsystem.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
	long countByUserId(UUID userId);

	Optional<Account> findByUserIdAndCurrency(UUID userId, String currency);

	Optional<Account> findByUserId(UUID userId);

	Page<Account> findAllByUserId(UUID id, Pageable pageable);

	Optional<Account> findByAccountName(String accountName);

	Page<Account> findByAccountNameContainingIgnoreCase(String accountName, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT a FROM Account a WHERE a.id = :id")
	Optional<Account> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);
}