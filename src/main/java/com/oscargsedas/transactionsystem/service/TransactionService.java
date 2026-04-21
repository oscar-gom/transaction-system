package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.EntityDtoMapper;
import com.oscargsedas.transactionsystem.dto.TransactionDto;
import com.oscargsedas.transactionsystem.dto.TransactionRequest;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.Transaction;
import com.oscargsedas.transactionsystem.entity.TransactionStatus;
import com.oscargsedas.transactionsystem.exception.*;
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
	private final EntityDtoMapper entityDtoMapper;

	@Retryable(
			retryFor = TransactionProcessingException.class,
			maxAttempts = 3,
			backoff = @Backoff(delay = 200, multiplier = 2)
	)
	@Transactional
	public TransactionDto createTransaction(TransactionRequest request) {
		Transaction transaction = createTransactionEntity(request);
		return entityDtoMapper.toTransactionDto(transaction);
	}

	public Transaction createTransactionEntity(TransactionRequest request) {
		UUID authenticatedUserId = accountService.getAuthenticatedUserId();
		Transaction existingTransaction = transactionRepository.findByIdempotencyKey(request.idempotencyKey())
				.orElse(null);
		if (existingTransaction != null) {
			validateTransactionOwnership(existingTransaction, authenticatedUserId);
			if (existingTransaction.getStatus() == TransactionStatus.COMPLETED) {
				throw new CompletedIdempotencyKeyException(request.idempotencyKey());
			}
			return existingTransaction;
		}

		try {
			if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
				throw new IllegalArgumentException("Amount must be greater than zero");
			}

			Account sender = accountService.getAccountEntityById(request.senderId());
			Account receiver = accountService.getAccountEntityById(request.receiverId());

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
	public TransactionDto recover(TransactionProcessingException ex, TransactionRequest request) {
		throw new TransactionRetriesExhaustedException(request.idempotencyKey(), ex);
	}

	private boolean isBusinessException(RuntimeException ex) {
		return ex instanceof IllegalArgumentException
				|| ex instanceof IllegalStateException
				|| ex instanceof ResourceNotFoundException
				|| ex instanceof ForbiddenAccessException
				|| ex instanceof CompletedIdempotencyKeyException;
	}

	public TransactionDto getTransactionById(UUID transactionId) {
		UUID authenticatedUserId = accountService.getAuthenticatedUserId();
		Transaction transaction = transactionRepository.findById(transactionId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
		validateTransactionOwnership(transaction, authenticatedUserId);
		return entityDtoMapper.toTransactionDto(transaction);
	}

	private void validateTransactionOwnership(Transaction transaction, UUID authenticatedUserId) {
		if (transaction.getSenderAccount() == null || transaction.getSenderAccount().getUser() == null
				|| transaction.getSenderAccount().getUser().getId() == null
				|| !transaction.getSenderAccount().getUser().getId().equals(authenticatedUserId)) {
			throw new ForbiddenAccessException("You do not have permission to access this transaction");
		}
		if (transaction.getReceiverAccount() == null || transaction.getReceiverAccount().getUser() == null
				|| transaction.getReceiverAccount().getUser().getId() == null
				|| !transaction.getReceiverAccount().getUser().getId().equals(authenticatedUserId)) {
			throw new ForbiddenAccessException("You do not have permission to access this transaction");
		}
	}
}
