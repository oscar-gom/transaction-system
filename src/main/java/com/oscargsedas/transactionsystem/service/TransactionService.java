package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.TransactionRequest;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.Transaction;
import com.oscargsedas.transactionsystem.entity.TransactionStatus;
import com.oscargsedas.transactionsystem.exception.CompletedIdempotencyKeyException;
import com.oscargsedas.transactionsystem.exception.ResourceNotFoundException;
import com.oscargsedas.transactionsystem.exception.TransactionProcessingException;
import com.oscargsedas.transactionsystem.exception.TransactionRetriesExhaustedException;
import com.oscargsedas.transactionsystem.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
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

	@Retryable(
			retryFor = TransactionProcessingException.class,
			maxAttempts = 3,
			backoff = @Backoff(delay = 200, multiplier = 2)
	)
	@Transactional
	public Transaction createTransaction(TransactionRequest request) {
		Transaction existingTransaction = transactionRepository.findByIdempotencyKey(request.idempotencyKey())
				.orElse(null);
		if (existingTransaction != null) {
			if (existingTransaction.getStatus() == TransactionStatus.COMPLETED) {
				throw new CompletedIdempotencyKeyException(request.idempotencyKey());
			}
			return existingTransaction;
		}

		try {
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
			transaction.setStatus(TransactionStatus.PENDING);

			Transaction savedTransaction = transactionRepository.save(transaction);
			ledgerLineService.createLedgerLinesForTransaction(savedTransaction);

			long ledgerLineCount = ledgerLineService.countByTransactionId(savedTransaction.getId());
			if (ledgerLineCount < 2) {
				throw new IllegalStateException("A completed transaction must have at least two ledger lines");
			}

			BigDecimal transactionBalance = ledgerLineService.getTransactionBalance(savedTransaction.getId());
			if (transactionBalance.compareTo(BigDecimal.ZERO) != 0) {
				throw new IllegalStateException("Ledger is not balanced for transaction " + savedTransaction.getId());
			}

			savedTransaction.setStatus(TransactionStatus.COMPLETED);
			return transactionRepository.save(savedTransaction);
		} catch (RuntimeException ex) {
			if (isBusinessException(ex)) {
				throw ex;
			}
			throw new TransactionProcessingException("Transient technical error while processing transaction", ex);
		}
	}

	@Recover
	public Transaction recover(TransactionProcessingException ex, TransactionRequest request) {
		throw new TransactionRetriesExhaustedException(request.idempotencyKey(), ex);
	}

	private boolean isBusinessException(RuntimeException ex) {
		return ex instanceof IllegalArgumentException
				|| ex instanceof IllegalStateException
				|| ex instanceof ResourceNotFoundException
				|| ex instanceof CompletedIdempotencyKeyException;
	}

	public Transaction getTransactionById(UUID transactionId) {
		return transactionRepository.findById(transactionId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
	}
}
