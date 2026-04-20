package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.TransactionRequest;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.Transaction;
import com.oscargsedas.transactionsystem.entity.TransactionStatus;
import com.oscargsedas.transactionsystem.exception.CompletedIdempotencyKeyException;
import com.oscargsedas.transactionsystem.exception.TransactionProcessingException;
import com.oscargsedas.transactionsystem.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

	@InjectMocks
	private TransactionService transactionService;

	@Test
	void createTransactionThrowsWhenIdempotencyKeyAlreadyCompleted() {
		UUID idempotencyKey = UUID.randomUUID();
		Transaction existing = new Transaction();
		existing.setId(UUID.randomUUID());
		existing.setIdempotencyKey(idempotencyKey);
		existing.setStatus(TransactionStatus.COMPLETED);

		when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));

		TransactionRequest request = new TransactionRequest(UUID.randomUUID(), UUID.randomUUID(), idempotencyKey, new BigDecimal("50.00"));
		assertThrows(CompletedIdempotencyKeyException.class, () -> transactionService.createTransaction(request));

		verify(transactionRepository, never()).save(any(Transaction.class));
		verifyNoInteractions(accountService);
		verifyNoInteractions(ledgerLineService);
	}

	@Test
	void createTransactionReturnsExistingWhenIdempotencyKeyAlreadyExistsAndNotCompleted() {
		UUID idempotencyKey = UUID.randomUUID();
		Transaction existing = new Transaction();
		existing.setId(UUID.randomUUID());
		existing.setIdempotencyKey(idempotencyKey);
		existing.setStatus(TransactionStatus.PENDING);

		when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));

		TransactionRequest request = new TransactionRequest(UUID.randomUUID(), UUID.randomUUID(), idempotencyKey, new BigDecimal("50.00"));
		Transaction result = transactionService.createTransaction(request);

		assertSame(existing, result);
		verify(transactionRepository, never()).save(any(Transaction.class));
		verifyNoInteractions(accountService);
		verifyNoInteractions(ledgerLineService);
	}

	@Test
	void createTransactionCompletesWhenLedgerIsBalanced() {
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();
		UUID idempotencyKey = UUID.randomUUID();
		BigDecimal amount = new BigDecimal("25.00");

		Account sender = new Account();
		sender.setId(senderId);
		Account receiver = new Account();
		receiver.setId(receiverId);

		TransactionRequest request = new TransactionRequest(senderId, receiverId, idempotencyKey, amount);

		when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
		when(accountService.getAccountById(senderId)).thenReturn(sender);
		when(accountService.getAccountById(receiverId)).thenReturn(receiver);
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

		Transaction result = transactionService.createTransaction(request);

		assertEquals(TransactionStatus.COMPLETED, result.getStatus());
		assertEquals(idempotencyKey, result.getIdempotencyKey());
		verify(transactionRepository, times(2)).save(any(Transaction.class));
		verify(ledgerLineService).createLedgerLinesForTransaction(any(Transaction.class));
	}

	@Test
	void createTransactionFailsWhenSenderHasInsufficientFunds() {
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();
		UUID idempotencyKey = UUID.randomUUID();

		Account sender = new Account();
		sender.setId(senderId);
		Account receiver = new Account();
		receiver.setId(receiverId);

		when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
		when(accountService.getAccountById(senderId)).thenReturn(sender);
		when(accountService.getAccountById(receiverId)).thenReturn(receiver);
		when(ledgerLineService.getAccountBalance(senderId)).thenReturn(new BigDecimal("10.00"));

		TransactionRequest request = new TransactionRequest(senderId, receiverId, idempotencyKey, new BigDecimal("50.00"));

		assertThrows(IllegalArgumentException.class, () -> transactionService.createTransaction(request));
		verify(transactionRepository, never()).save(any(Transaction.class));
		verify(ledgerLineService, never()).createLedgerLinesForTransaction(any(Transaction.class));
	}

	@Test
	void createTransactionFailsWhenLedgerLineCountIsLessThanTwo() {
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();
		UUID idempotencyKey = UUID.randomUUID();

		Account sender = new Account();
		sender.setId(senderId);
		Account receiver = new Account();
		receiver.setId(receiverId);

		when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
		when(accountService.getAccountById(senderId)).thenReturn(sender);
		when(accountService.getAccountById(receiverId)).thenReturn(receiver);
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
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();
		UUID idempotencyKey = UUID.randomUUID();

		Account sender = new Account();
		sender.setId(senderId);
		Account receiver = new Account();
		receiver.setId(receiverId);

		when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
		when(accountService.getAccountById(senderId)).thenReturn(sender);
		when(accountService.getAccountById(receiverId)).thenReturn(receiver);
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
		UUID senderId = UUID.randomUUID();
		UUID receiverId = UUID.randomUUID();
		UUID idempotencyKey = UUID.randomUUID();

		Account sender = new Account();
		sender.setId(senderId);
		Account receiver = new Account();
		receiver.setId(receiverId);

		when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
		when(accountService.getAccountById(senderId)).thenReturn(sender);
		when(accountService.getAccountById(receiverId)).thenReturn(receiver);
		when(ledgerLineService.getAccountBalance(senderId)).thenReturn(new BigDecimal("90.00"));
		when(transactionRepository.save(any(Transaction.class))).thenThrow(new RuntimeException("db temporary error"));

		TransactionRequest request = new TransactionRequest(senderId, receiverId, idempotencyKey, new BigDecimal("20.00"));

		assertThrows(TransactionProcessingException.class, () -> transactionService.createTransaction(request));
		verify(transactionRepository, times(1)).save(any(Transaction.class));
	}
}



