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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
	private final TransactionRepository transactionRepository;
	private final AccountService accountService;
	private final LedgerLineService ledgerLineService;
	private final EntityDtoMapper entityDtoMapper;

	@Retryable(
			retryFor = TransactionProcessingException.class,
			noRetryFor = {
					IllegalArgumentException.class,
					IllegalStateException.class,
					ResourceNotFoundException.class,
					ForbiddenAccessException.class,
					CompletedIdempotencyKeyException.class
			},
			maxAttempts = 3,
			backoff = @Backoff(delay = 200, multiplier = 2),
			recover = "recoverCreateTransaction"
	)
	@Transactional
	public TransactionDto createTransaction(TransactionRequest request) {
		Transaction transaction = createTransactionEntity(request);
		return entityDtoMapper.toTransactionDto(transaction);
	}

	public Transaction createTransactionEntity(TransactionRequest request) {
		UUID authenticatedUserId = accountService.getAuthenticatedUserId();
		log.info("TX_FLOW create idempotencyKey={} userId={} senderId={} receiverId={} amount={}",
				request.idempotencyKey(), authenticatedUserId, request.senderId(), request.receiverId(), request.amount());

		Transaction existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
		if (existing != null) {
			return handleExistingIdempotent(existing, request.idempotencyKey(), authenticatedUserId);
		}
		if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
			log.warn("TX_FLOW invalid amount={} idempotencyKey={}", request.amount(), request.idempotencyKey());
			throw new IllegalArgumentException("Amount must be greater than zero");
		}

		Account sender;
		try {
			sender = accountService.getAccountEntityById(request.senderId());
		} catch (ForbiddenAccessException ex) {
			log.warn("TX_FLOW forbidden sender access userId={} senderId={}", authenticatedUserId, request.senderId());
			throw ex;
		}
		BigDecimal senderBalance = ledgerLineService.getAccountBalance(sender.getId());
		log.info("TX_FLOW sender balance accountId={} balance={} amount={}", sender.getId(), senderBalance, request.amount());
		if (senderBalance.compareTo(request.amount()) < 0) {
			log.warn("TX_FLOW insufficient funds accountId={} balance={} amount={}", sender.getId(), senderBalance, request.amount());
			throw new IllegalArgumentException("Insufficient funds");
		}

		Account receiver = loadReceiverAccount(request.receiverId());
		Transaction transaction = new Transaction();
		transaction.setSenderAccount(sender);
		transaction.setReceiverAccount(receiver);
		transaction.setIdempotencyKey(request.idempotencyKey());
		transaction.setAmount(request.amount());
		transaction.setStatus(TransactionStatus.PENDING);

		return persistAndCompleteTransaction(transaction, request, authenticatedUserId);
	}

	private Transaction handleExistingIdempotent(Transaction existing, UUID idempotencyKey, UUID authenticatedUserId) {
		validateSenderOwnership(existing, authenticatedUserId);
		if (existing.getStatus() == TransactionStatus.COMPLETED) {
			throw new CompletedIdempotencyKeyException(idempotencyKey);
		}
		return existing;
	}

	private Account loadReceiverAccount(UUID receiverId) {
		return accountService.getAnyAccountEntityById(receiverId);
	}

	private Transaction persistAndCompleteTransaction(Transaction transaction, TransactionRequest request, UUID authenticatedUserId) {
		try {
			Transaction savedTransaction = transactionRepository.save(transaction);
			ledgerLineService.createLedgerLinesForTransaction(savedTransaction);

			long ledgerLineCount = ledgerLineService.countByTransactionId(savedTransaction.getId());
			if (ledgerLineCount < 2) {
				log.error("TX_FLOW invalid ledger count transactionId={} count={}", savedTransaction.getId(), ledgerLineCount);
				throw new IllegalStateException("A completed transaction must have at least two ledger lines");
			}

			BigDecimal transactionBalance = ledgerLineService.getTransactionBalance(savedTransaction.getId());
			if (transactionBalance.compareTo(BigDecimal.ZERO) != 0) {
				log.error("TX_FLOW unbalanced ledger transactionId={} balance={}", savedTransaction.getId(), transactionBalance);
				throw new IllegalStateException("Ledger is not balanced for transaction " + savedTransaction.getId());
			}

			savedTransaction.setStatus(TransactionStatus.COMPLETED);
			log.info("TX_FLOW completed transactionId={} idempotencyKey={}", savedTransaction.getId(), request.idempotencyKey());
			return transactionRepository.save(savedTransaction);
		} catch (DataIntegrityViolationException ex) {
			log.warn("TX_FLOW idempotency conflict key={}", request.idempotencyKey(), ex);
			return resolveAfterIdempotencyConflict(request, authenticatedUserId, ex);
		} catch (TransientDataAccessException ex) {
			log.warn("TX_FLOW transient technical error key={}", request.idempotencyKey(), ex);
			throw new TransactionProcessingException("Transient technical error while processing transaction", ex);
		} catch (DataAccessException ex) {
			log.error("TX_FLOW non-transient data access error key={}", request.idempotencyKey(), ex);
			throw new IllegalStateException("Failed to persist transaction due to a non-transient data access error", ex);
		}
	}

	private Transaction resolveAfterIdempotencyConflict(TransactionRequest request, UUID authenticatedUserId, DataIntegrityViolationException cause) {
		Transaction existingTransaction = transactionRepository.findByIdempotencyKey(request.idempotencyKey())
				.orElseThrow(() -> new IllegalStateException("Idempotency conflict detected but transaction could not be recovered", cause));
		return handleExistingIdempotent(existingTransaction, request.idempotencyKey(), authenticatedUserId);
	}

	@Recover
	public TransactionDto recoverCreateTransaction(TransactionProcessingException ex, TransactionRequest request) {
		log.error("Recover invoked for idempotencyKey={} with exception type={} and message={}",
				request.idempotencyKey(),
				ex.getClass().getName(),
				ex.getMessage(),
				ex);
		throw new TransactionRetriesExhaustedException(request.idempotencyKey(), ex);
	}


	public TransactionDto getTransactionById(UUID transactionId) {
		UUID authenticatedUserId = accountService.getAuthenticatedUserId();
		Transaction transaction = transactionRepository.findById(transactionId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
		validateSenderOwnership(transaction, authenticatedUserId);
		return entityDtoMapper.toTransactionDto(transaction);
	}

	private void validateSenderOwnership(Transaction transaction, UUID authenticatedUserId) {
		if (transaction.getSenderAccount() == null || transaction.getSenderAccount().getUser() == null
				|| transaction.getSenderAccount().getUser().getId() == null
				|| !transaction.getSenderAccount().getUser().getId().equals(authenticatedUserId)) {
			throw new ForbiddenAccessException("You do not have permission to access this transaction");
		}
	}
}
