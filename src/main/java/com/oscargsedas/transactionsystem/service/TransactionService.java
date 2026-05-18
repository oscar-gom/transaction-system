package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.EntityDtoMapper;
import com.oscargsedas.transactionsystem.dto.TransactionDto;
import com.oscargsedas.transactionsystem.dto.TransactionRequest;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.Transaction;
import com.oscargsedas.transactionsystem.entity.TransactionStatus;
import com.oscargsedas.transactionsystem.entity.User;
import com.oscargsedas.transactionsystem.exception.*;
import com.oscargsedas.transactionsystem.repository.TransactionRepository;
import com.oscargsedas.transactionsystem.util.AuthenticatedUserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
	public static final int PAGE_SIZE = 10;
	private final TransactionRepository transactionRepository;
	private final AccountService accountService;
	private final LedgerLineService ledgerLineService;
	private final EntityDtoMapper entityDtoMapper;
	private final AuthenticatedUserUtil authenticatedUserUtil;

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
		return entityDtoMapper.toTransactionDto(createTransactionEntity(request));
	}

	public Page<TransactionDto> getTransactionsForAuthenticatedUser(Pageable pageable) {
		if (pageable == null) {
			pageable = PageRequest.of(0, PAGE_SIZE);
		}
		if (pageable.getPageNumber() < 0) {
			throw new IllegalArgumentException("Page index must be zero or positive");
		}

		Pageable normalizedPageable = PageRequest.of(pageable.getPageNumber(), PAGE_SIZE, pageable.getSort());
		UUID userId = authenticatedUserUtil.getAuthenticatedUserId();
		return transactionRepository.findBySenderAccount_User_IdOrReceiverAccount_User_Id(userId, userId, normalizedPageable)
				.map(entityDtoMapper::toTransactionDto);
	}

	public TransactionDto getTransactionById(UUID transactionId) {
		UUID userId = authenticatedUserUtil.getAuthenticatedUserId();
		Transaction transaction = transactionRepository.findById(transactionId)
				.orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
		validateTransactionAccess(transaction, userId);
		return entityDtoMapper.toTransactionDto(transaction);
	}

	@Recover
	public void recoverCreateTransaction(TransactionProcessingException ex, TransactionRequest request) {
		log.error("TX_FLOW recover exhausted idempotencyKey={} exceptionType={} message={}",
				request.idempotencyKey(), ex.getClass().getName(), ex.getMessage(), ex);
		throw new TransactionRetriesExhaustedException(request.idempotencyKey(), ex);
	}

	public Transaction createTransactionEntity(TransactionRequest request) {
		UUID userId = authenticatedUserUtil.getAuthenticatedUserId();
		log.info("TX_FLOW create idempotencyKey={} userId={} senderId={} receiverId={} amount={}",
				request.idempotencyKey(), userId, request.senderId(), request.receiverId(), request.amount());

		return transactionRepository.findByIdempotencyKey(request.idempotencyKey())
				.map(existing -> resolveIdempotentRequest(existing, request.idempotencyKey(), userId))
				.orElseGet(() -> createNewTransaction(request, userId));
	}

	private Transaction createNewTransaction(TransactionRequest request, UUID userId) {
		validateTransactionRequest(request);

		Account sender = accountService.getAccountEntityById(request.senderId());
		validateSufficientFunds(sender.getId(), request.amount(), request.idempotencyKey());

		Account receiver = accountService.getAnyAccountEntityById(request.receiverId());
		Transaction tx = buildPendingTransaction(sender, receiver, request);

		return persistAndCompleteTransaction(tx, request, userId);
	}

	private Transaction resolveIdempotentRequest(Transaction existing, UUID idempotencyKey, UUID userId) {
		validateTransactionAccess(existing, userId);
		if (existing.getStatus() == TransactionStatus.COMPLETED)
			throw new CompletedIdempotencyKeyException(idempotencyKey);
		return existing;
	}

	private Transaction resolveAfterIdempotencyConflict(TransactionRequest request, UUID userId,
	                                                    DataIntegrityViolationException cause) {
		Transaction existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey())
				.orElseThrow(() -> new IllegalStateException(
						"Idempotency conflict detected but transaction could not be recovered", cause));
		return resolveIdempotentRequest(existing, request.idempotencyKey(), userId);
	}

	private Transaction persistAndCompleteTransaction(Transaction tx, TransactionRequest request, UUID userId) {
		try {
			Transaction saved = transactionRepository.save(tx);
			ledgerLineService.createLedgerLinesForTransaction(saved);
			validateLedgerIntegrity(saved);

			saved.setStatus(TransactionStatus.COMPLETED);
			log.info("TX_FLOW completed transactionId={} idempotencyKey={}", saved.getId(), request.idempotencyKey());
			return transactionRepository.save(saved);

		} catch (DataIntegrityViolationException ex) {
			log.warn("TX_FLOW idempotency conflict key={}", request.idempotencyKey(), ex);
			return resolveAfterIdempotencyConflict(request, userId, ex);

		} catch (TransientDataAccessException ex) {
			log.warn("TX_FLOW transient error key={}", request.idempotencyKey(), ex);
			throw new TransactionProcessingException("Transient technical error while processing transaction", ex);

		} catch (DataAccessException ex) {
			log.error("TX_FLOW non-transient data access error key={}", request.idempotencyKey(), ex);
			throw new IllegalStateException("Failed to persist transaction due to a non-transient data access error", ex);
		}
	}

	private void validateLedgerIntegrity(Transaction tx) {
		long count = ledgerLineService.countByTransactionId(tx.getId());
		if (count < 2) {
			log.error("TX_FLOW invalid ledger count transactionId={} count={}", tx.getId(), count);
			throw new IllegalStateException("A completed transaction must have at least two ledger lines");
		}

		BigDecimal balance = ledgerLineService.getTransactionBalance(tx.getId());
		if (balance.compareTo(BigDecimal.ZERO) != 0) {
			log.error("TX_FLOW unbalanced ledger transactionId={} balance={}", tx.getId(), balance);
			throw new IllegalStateException("Ledger is not balanced for transaction " + tx.getId());
		}
	}

	private Transaction buildPendingTransaction(Account sender, Account receiver, TransactionRequest request) {
		var tx = new Transaction();
		tx.setSenderAccount(sender);
		tx.setReceiverAccount(receiver);
		tx.setIdempotencyKey(request.idempotencyKey());
		tx.setAmount(request.amount());
		tx.setStatus(TransactionStatus.PENDING);
		return tx;
	}

	private void validateTransactionRequest(TransactionRequest request) {
		if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
			log.warn("TX_FLOW invalid amount={} idempotencyKey={}", request.amount(), request.idempotencyKey());
			throw new IllegalArgumentException("Amount must be greater than zero");
		}
	}

	private void validateSufficientFunds(UUID accountId, BigDecimal amount, UUID idempotencyKey) {
		BigDecimal balance = ledgerLineService.getAccountBalance(accountId);
		log.info("TX_FLOW sender balance accountId={} balance={} amount={}", accountId, balance, amount);
		if (balance.compareTo(amount) < 0) {
			log.warn("TX_FLOW insufficient funds accountId={} balance={} amount={}", accountId, balance, amount);
			throw new IllegalArgumentException("Insufficient funds");
		}
	}

	private void validateTransactionAccess(Transaction transaction, UUID userId) {
		boolean isSender = Optional.ofNullable(transaction.getSenderAccount())
				.map(Account::getUser)
				.map(User::getId)
				.filter(userId::equals)
				.isPresent();
		boolean isReceiver = Optional.ofNullable(transaction.getReceiverAccount())
				.map(Account::getUser)
				.map(User::getId)
				.filter(userId::equals)
				.isPresent();

		if (!isSender && !isReceiver)
			throw new ForbiddenAccessException("You do not have permission to access this transaction");
	}
}