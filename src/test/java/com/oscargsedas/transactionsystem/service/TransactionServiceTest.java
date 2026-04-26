package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.EntityDtoMapper;
import com.oscargsedas.transactionsystem.dto.TransactionDto;
import com.oscargsedas.transactionsystem.dto.TransactionRequest;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.Transaction;
import com.oscargsedas.transactionsystem.entity.TransactionStatus;
import com.oscargsedas.transactionsystem.entity.User;
import com.oscargsedas.transactionsystem.exception.CompletedIdempotencyKeyException;
import com.oscargsedas.transactionsystem.exception.ForbiddenAccessException;
import com.oscargsedas.transactionsystem.exception.TransactionProcessingException;
import com.oscargsedas.transactionsystem.exception.TransactionRetriesExhaustedException;
import com.oscargsedas.transactionsystem.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private AccountService accountService;

	@Mock
	private LedgerLineService ledgerLineService;

	@Mock
	private EntityDtoMapper entityDtoMapper;

	@InjectMocks
	private TransactionService transactionService;

	@Test
	void getTransactionsForAuthenticatedUserIncludesSentAndReceivedTransactions() {
		UUID authenticatedUserId = UUID.randomUUID();
		Pageable pageable = PageRequest.of(0, TransactionService.PAGE_SIZE);
		Transaction tx = new Transaction();
		tx.setId(UUID.randomUUID());
		TransactionDto dto = new TransactionDto(null, null, tx.getId(), null, null, UUID.randomUUID(), new BigDecimal("10.00"), TransactionStatus.COMPLETED);

		when(accountService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
		when(transactionRepository.findBySenderAccount_User_IdOrReceiverAccount_User_Id(authenticatedUserId, authenticatedUserId, pageable))
				.thenReturn(new PageImpl<>(List.of(tx), pageable, 1));
		when(entityDtoMapper.toTransactionDto(tx)).thenReturn(dto);

		Page<TransactionDto> result = transactionService.getTransactionsForAuthenticatedUser(pageable);

		assertEquals(1, result.getTotalElements());
		assertEquals(dto, result.getContent().getFirst());
		verify(transactionRepository).findBySenderAccount_User_IdOrReceiverAccount_User_Id(authenticatedUserId, authenticatedUserId, pageable);
	}

	@Test
	void createTransactionThrowsWhenIdempotencyKeyAlreadyCompleted() {
		UUID authenticatedUserId = UUID.randomUUID();
		UUID idempotencyKey = UUID.randomUUID();

		Account sender = accountOwnedBy(authenticatedUserId);
		Account receiver = accountOwnedBy(authenticatedUserId);

		Transaction existing = new Transaction();
		existing.setId(UUID.randomUUID());
		existing.setIdempotencyKey(idempotencyKey);
		existing.setStatus(TransactionStatus.COMPLETED);
		existing.setSenderAccount(sender);
		existing.setReceiverAccount(receiver);

		when(accountService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
		when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));

		TransactionRequest request = new TransactionRequest(sender.getId(), receiver.getId(), idempotencyKey, new BigDecimal("50.00"));
		assertThrows(CompletedIdempotencyKeyException.class, () -> transactionService.createTransaction(request));

		verify(transactionRepository, never()).save(any(Transaction.class));
		verifyNoInteractions(ledgerLineService);
	}

	@Test
	void createTransactionReturnsExistingWhenIdempotencyKeyAlreadyExistsAndNotCompleted() {
		UUID authenticatedUserId = UUID.randomUUID();
		UUID idempotencyKey = UUID.randomUUID();

		Account sender = accountOwnedBy(authenticatedUserId);
		Account receiver = accountOwnedBy(authenticatedUserId);

		Transaction existing = new Transaction();
		existing.setId(UUID.randomUUID());
		existing.setIdempotencyKey(idempotencyKey);
		existing.setStatus(TransactionStatus.PENDING);
		existing.setSenderAccount(sender);
		existing.setReceiverAccount(receiver);

		when(accountService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
		when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));

		TransactionDto mapped = new TransactionDto(null, null, existing.getId(), null, null, idempotencyKey, new BigDecimal("50.00"), TransactionStatus.PENDING);
		when(entityDtoMapper.toTransactionDto(existing)).thenReturn(mapped);

		TransactionRequest request = new TransactionRequest(sender.getId(), receiver.getId(), idempotencyKey, new BigDecimal("50.00"));
		TransactionDto result = transactionService.createTransaction(request);

		assertSame(mapped, result);
		verify(transactionRepository, never()).save(any(Transaction.class));
		verifyNoInteractions(ledgerLineService);
	}

	@Test
	void createTransactionFailsWhenExistingIdempotentTransactionDoesNotBelongToUser() {
		UUID authenticatedUserId = UUID.randomUUID();
		UUID idempotencyKey = UUID.randomUUID();

		Account sender = accountOwnedBy(UUID.randomUUID());
		Account receiver = accountOwnedBy(authenticatedUserId);

		Transaction existing = new Transaction();
		existing.setId(UUID.randomUUID());
		existing.setIdempotencyKey(idempotencyKey);
		existing.setStatus(TransactionStatus.PENDING);
		existing.setSenderAccount(sender);
		existing.setReceiverAccount(receiver);

		when(accountService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
		when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));

		TransactionRequest request = new TransactionRequest(sender.getId(), receiver.getId(), idempotencyKey, new BigDecimal("10.00"));

		assertThrows(ForbiddenAccessException.class, () -> transactionService.createTransaction(request));
		verify(transactionRepository, never()).save(any(Transaction.class));
		verifyNoInteractions(ledgerLineService);
	}

	@Test
	void createTransactionCompletesWhenLedgerIsBalanced() {
		UUID authenticatedUserId = UUID.randomUUID();
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();
		UUID idempotencyKey = UUID.randomUUID();
		BigDecimal amount = new BigDecimal("25.00");

		Account sender = accountOwnedBy(authenticatedUserId);
		sender.setId(senderId);
		Account receiver = accountOwnedBy(authenticatedUserId);
		receiver.setId(receiverId);

		TransactionRequest request = new TransactionRequest(senderId, receiverId, idempotencyKey, amount);

		when(accountService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
		when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
		when(accountService.getAccountEntityById(senderId)).thenReturn(sender);
		when(accountService.getAnyAccountEntityById(receiverId)).thenReturn(receiver);
		when(ledgerLineService.getAccountBalance(senderId)).thenReturn(new BigDecimal("100.00"));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
			Transaction transaction = invocation.getArgument(0);
			if (transaction.getId() == null) {
				transaction.setId(UUID.randomUUID());
			}
			return transaction;
		});
		when(ledgerLineService.countByTransactionId(any(UUID.class))).thenReturn(2L);
		when(ledgerLineService.getTransactionBalance(any(UUID.class))).thenReturn(BigDecimal.ZERO);

		TransactionDto mapped = new TransactionDto(null, null, UUID.randomUUID(), null, null, idempotencyKey, amount, TransactionStatus.COMPLETED);
		when(entityDtoMapper.toTransactionDto(any(Transaction.class))).thenReturn(mapped);

		TransactionDto result = transactionService.createTransaction(request);

		assertEquals(TransactionStatus.COMPLETED, result.status());
		assertEquals(idempotencyKey, result.idempotencyKey());
		verify(transactionRepository, times(2)).save(any(Transaction.class));
		verify(ledgerLineService).createLedgerLinesForTransaction(any(Transaction.class));
	}

	@Test
	void createTransactionFailsWhenSenderHasInsufficientFunds() {
		UUID authenticatedUserId = UUID.randomUUID();
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();
		UUID idempotencyKey = UUID.randomUUID();

		Account sender = accountOwnedBy(authenticatedUserId);
		sender.setId(senderId);
		Account receiver = accountOwnedBy(authenticatedUserId);
		receiver.setId(receiverId);

		when(accountService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
		when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
		when(accountService.getAccountEntityById(senderId)).thenReturn(sender);
		when(ledgerLineService.getAccountBalance(senderId)).thenReturn(new BigDecimal("10.00"));

		TransactionRequest request = new TransactionRequest(senderId, receiverId, idempotencyKey, new BigDecimal("50.00"));

		assertThrows(IllegalArgumentException.class, () -> transactionService.createTransaction(request));
		verify(transactionRepository, never()).save(any(Transaction.class));
		verify(ledgerLineService, never()).createLedgerLinesForTransaction(any(Transaction.class));
		verify(accountService, never()).getAnyAccountEntityById(receiverId);
	}

	@Test
	void createTransactionChecksInsufficientFundsBeforeReceiverOwnership() {
		UUID authenticatedUserId = UUID.randomUUID();
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();
		UUID idempotencyKey = UUID.randomUUID();

		Account sender = accountOwnedBy(authenticatedUserId);
		sender.setId(senderId);

		when(accountService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
		when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
		when(accountService.getAccountEntityById(senderId)).thenReturn(sender);
		when(ledgerLineService.getAccountBalance(senderId)).thenReturn(new BigDecimal("10.00"));

		TransactionRequest request = new TransactionRequest(senderId, receiverId, idempotencyKey, new BigDecimal("50.00"));

		assertThrows(IllegalArgumentException.class, () -> transactionService.createTransaction(request));
		verify(accountService, never()).getAnyAccountEntityById(receiverId);
	}

	@Test
	void createTransactionFailsWhenLedgerLineCountIsLessThanTwo() {
		UUID authenticatedUserId = UUID.randomUUID();
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();
		UUID idempotencyKey = UUID.randomUUID();

		Account sender = accountOwnedBy(authenticatedUserId);
		sender.setId(senderId);
		Account receiver = accountOwnedBy(authenticatedUserId);
		receiver.setId(receiverId);

		when(accountService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
		when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
		when(accountService.getAccountEntityById(senderId)).thenReturn(sender);
		when(accountService.getAnyAccountEntityById(receiverId)).thenReturn(receiver);
		when(ledgerLineService.getAccountBalance(senderId)).thenReturn(new BigDecimal("90.00"));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
			Transaction transaction = invocation.getArgument(0);
			if (transaction.getId() == null) {
				transaction.setId(UUID.randomUUID());
			}
			return transaction;
		});
		when(ledgerLineService.countByTransactionId(any(UUID.class))).thenReturn(1L);

		TransactionRequest request = new TransactionRequest(senderId, receiverId, idempotencyKey, new BigDecimal("20.00"));

		assertThrows(IllegalStateException.class, () -> transactionService.createTransaction(request));
		verify(transactionRepository, times(1)).save(any(Transaction.class));
	}

	@Test
	void createTransactionFailsWhenLedgerBalanceIsNotZero() {
		UUID authenticatedUserId = UUID.randomUUID();
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();
		UUID idempotencyKey = UUID.randomUUID();

		Account sender = accountOwnedBy(authenticatedUserId);
		sender.setId(senderId);
		Account receiver = accountOwnedBy(authenticatedUserId);
		receiver.setId(receiverId);

		when(accountService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
		when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
		when(accountService.getAccountEntityById(senderId)).thenReturn(sender);
		when(accountService.getAnyAccountEntityById(receiverId)).thenReturn(receiver);
		when(ledgerLineService.getAccountBalance(senderId)).thenReturn(new BigDecimal("90.00"));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
			Transaction transaction = invocation.getArgument(0);
			if (transaction.getId() == null) {
				transaction.setId(UUID.randomUUID());
			}
			return transaction;
		});
		when(ledgerLineService.countByTransactionId(any(UUID.class))).thenReturn(2L);
		when(ledgerLineService.getTransactionBalance(any(UUID.class))).thenReturn(new BigDecimal("1.00"));

		TransactionRequest request = new TransactionRequest(senderId, receiverId, idempotencyKey, new BigDecimal("20.00"));

		assertThrows(IllegalStateException.class, () -> transactionService.createTransaction(request));
		verify(transactionRepository, times(1)).save(any(Transaction.class));

		ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(captor.capture());
		assertEquals(TransactionStatus.PENDING, captor.getValue().getStatus());
	}

	@Test
	void createTransactionWrapsTechnicalErrorsAsRetryableException() {
		UUID authenticatedUserId = UUID.randomUUID();
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();
		UUID idempotencyKey = UUID.randomUUID();

		Account sender = accountOwnedBy(authenticatedUserId);
		sender.setId(senderId);
		Account receiver = accountOwnedBy(authenticatedUserId);
		receiver.setId(receiverId);

		when(accountService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
		when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
		when(accountService.getAccountEntityById(senderId)).thenReturn(sender);
		when(accountService.getAnyAccountEntityById(receiverId)).thenReturn(receiver);
		when(ledgerLineService.getAccountBalance(senderId)).thenReturn(new BigDecimal("90.00"));
		when(transactionRepository.save(any(Transaction.class)))
				.thenThrow(new TransientDataAccessResourceException("db temporary error"));

		TransactionRequest request = new TransactionRequest(senderId, receiverId, idempotencyKey, new BigDecimal("20.00"));

		assertThrows(TransactionProcessingException.class, () -> transactionService.createTransaction(request));
		verify(transactionRepository, times(1)).save(any(Transaction.class));
	}

	@Test
	void getTransactionByIdFailsWhenSenderAccountBelongsToAnotherUser() {
		UUID authenticatedUserId = UUID.randomUUID();
		UUID transactionId = UUID.randomUUID();
		Account sender = accountOwnedBy(UUID.randomUUID());
		Account receiver = accountOwnedBy(authenticatedUserId);

		Transaction transaction = new Transaction();
		transaction.setId(transactionId);
		transaction.setSenderAccount(sender);
		transaction.setReceiverAccount(receiver);

		when(accountService.getAuthenticatedUserId()).thenReturn(authenticatedUserId);
		when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

		assertThrows(ForbiddenAccessException.class, () -> transactionService.getTransactionById(transactionId));
		verify(entityDtoMapper, never()).toTransactionDto(any(Transaction.class));
	}

	@Test
	void recoverCreateTransactionThrowsRetriesExhaustedForTechnicalCause() {
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();
		UUID idempotencyKey = UUID.randomUUID();
		TransactionRequest request = new TransactionRequest(senderId, receiverId, idempotencyKey, new BigDecimal("50.00"));

		TransactionProcessingException retryable = new TransactionProcessingException(
				"Transient technical error while processing transaction",
				new RuntimeException("db down")
		);

		TransactionRetriesExhaustedException thrown = assertThrows(
				TransactionRetriesExhaustedException.class,
				() -> transactionService.recoverCreateTransaction(retryable, request)
		);
		assertTrue(thrown.getMessage().contains(idempotencyKey.toString()));
	}


	private Account accountOwnedBy(UUID userId) {
		User user = new User();
		user.setId(userId);
		Account account = new Account();
		account.setId(UUID.randomUUID());
		account.setUser(user);
		return account;
	}
}
