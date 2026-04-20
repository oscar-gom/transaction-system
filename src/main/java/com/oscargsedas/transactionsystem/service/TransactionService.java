package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.TransactionRequest;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.Transaction;
import com.oscargsedas.transactionsystem.exception.ResourceNotFoundException;
import com.oscargsedas.transactionsystem.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {
	private final TransactionRepository transactionRepository;
	private final AccountService accountService;
	private final LedgerLineService ledgerLineService;

	@Transactional
	public void createTransaction(TransactionRequest request) {
		if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Amount must be greater than zero");
		}

		Account sender = accountService.getAccountById(request.senderId());
		Account receiver = accountService.getAccountById(request.receiverId());

		BigDecimal senderBalance = ledgerLineService.getAccountBalance(sender.getId());
		if (senderBalance.compareTo(request.amount()) < 0) {
			throw new IllegalArgumentException("Insufficient funds in sender's account");
		}

		Transaction transaction = new Transaction();
		transaction.setSenderAccount(sender);
		transaction.setReceiverAccount(receiver);
		transaction.setIdempotencyKey(request.idempotencyKey());
		transaction.setAmount(request.amount());

		Transaction savedTransaction = transactionRepository.save(transaction);
		ledgerLineService.createLedgerLinesForTransaction(savedTransaction);
	}

	public Transaction getTransactionById(UUID transactionId) {
		return transactionRepository.findById(transactionId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
	}
}
