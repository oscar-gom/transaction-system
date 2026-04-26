package com.oscargsedas.transactionsystem.repository;

import com.oscargsedas.transactionsystem.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
	Optional<Transaction> findByIdempotencyKey(UUID idempotencyKey);

	Page<Transaction> findBySenderAccount_User_IdOrReceiverAccount_User_Id(UUID senderUserId, UUID receiverUserId, Pageable pageable);
}