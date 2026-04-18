package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.TransactionRequest;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.Transaction;
import com.oscargsedas.transactionsystem.exception.ResourceNotFoundException;
import com.oscargsedas.transactionsystem.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {
	private final TransactionRepository transactionRepository;
	private final AccountService accountService;

	public void createTransaction(TransactionRequest request) {
		Account sender = accountService.getAccountById(request.senderId());
		Account receiver = accountService.getAccountById(request.receiverId());

		Transaction transaction = new Transaction();
		transaction.setSenderAccount(sender);
		transaction.setReceiverAccount(receiver);
		transaction.setIdempotencyKey(request.idempotencyKey());

		transactionRepository.save(transaction);
	}

	public Transaction getTransactionById(UUID transactionId) {
		return transactionRepository.findById(transactionId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
	}
}
