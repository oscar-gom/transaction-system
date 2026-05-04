package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.AccountRequest;
import com.oscargsedas.transactionsystem.dto.EntityDtoMapper;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.Transaction;
import com.oscargsedas.transactionsystem.entity.TransactionStatus;
import com.oscargsedas.transactionsystem.entity.User;
import com.oscargsedas.transactionsystem.repository.AccountRepository;
import com.oscargsedas.transactionsystem.repository.TransactionRepository;
import com.oscargsedas.transactionsystem.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private EntityDtoMapper entityDtoMapper;

	@Mock
	private LedgerLineService ledgerLineService;

	@Mock
	private SystemTreasuryAccountService systemTreasuryAccountService;

	@Mock
	private WelcomeBonusProperties welcomeBonusProperties;

	@InjectMocks
	private AccountService accountService;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createAccountAppliesWelcomeBonusOnFirstAccount() {
		UUID userId = UUID.randomUUID();
		User authenticatedUser = new User();
		authenticatedUser.setId(userId);
		authenticatedUser.setEmail("user@example.com");

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("user@example.com", "n/a")
		);

		when(userRepository.findByEmail("user@example.com")).thenReturn(authenticatedUser);
		when(accountRepository.countByUserId(userId)).thenReturn(0L);
		when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
			Account account = invocation.getArgument(0);
			if (account.getId() == null) {
				account.setId(UUID.randomUUID());
			}
			return account;
		});
		Account treasuryAccount = new Account();
		treasuryAccount.setId(UUID.randomUUID());
		treasuryAccount.setCurrency("EUR");
		when(systemTreasuryAccountService.getOrCreateTreasuryAccount("EUR")).thenReturn(treasuryAccount);
		when(welcomeBonusProperties.getAmount()).thenReturn(new BigDecimal("5000.00"));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
			Transaction transaction = invocation.getArgument(0);
			if (transaction.getId() == null) {
				transaction.setId(UUID.randomUUID());
			}
			return transaction;
		});
		when(entityDtoMapper.toAccountDto(any(Account.class))).thenReturn(null);

		accountService.createAccount(new AccountRequest("EUR"));

		ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(transactionCaptor.capture());
		Transaction transaction = transactionCaptor.getValue();

		assertEquals(new BigDecimal("5000.00"), transaction.getAmount());
		assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
		assertEquals(userId, transaction.getReceiverAccount().getUser().getId());
		verify(ledgerLineService, times(1)).createLedgerLinesForTransaction(any(Transaction.class));
	}

	@Test
	void createAccountDoesNotApplyWelcomeBonusAfterFirstAccount() {
		UUID userId = UUID.randomUUID();
		User authenticatedUser = new User();
		authenticatedUser.setId(userId);
		authenticatedUser.setEmail("user@example.com");

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("user@example.com", "n/a")
		);

		when(userRepository.findByEmail("user@example.com")).thenReturn(authenticatedUser);
		when(accountRepository.countByUserId(userId)).thenReturn(1L);
		when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
			Account account = invocation.getArgument(0);
			if (account.getId() == null) {
				account.setId(UUID.randomUUID());
			}
			return account;
		});
		when(entityDtoMapper.toAccountDto(any(Account.class))).thenReturn(null);

		accountService.createAccount(new AccountRequest("EUR"));

		verify(transactionRepository, never()).save(any(Transaction.class));
		verify(ledgerLineService, never()).createLedgerLinesForTransaction(any(Transaction.class));
	}

	@Test
	void getAccountBalanceForAuthenticatedUserReturnsBalanceOnlyForOwnedAccount() {
		UUID userId = UUID.randomUUID();
		UUID accountId = UUID.randomUUID();
		User authenticatedUser = new User();
		authenticatedUser.setId(userId);
		authenticatedUser.setEmail("user@example.com");

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("user@example.com", "n/a")
		);

		Account account = new Account();
		account.setId(accountId);
		account.setUser(authenticatedUser);
		account.setCurrency("EUR");

		when(userRepository.findByEmail("user@example.com")).thenReturn(authenticatedUser);
		when(accountRepository.findById(accountId)).thenReturn(java.util.Optional.of(account));
		when(ledgerLineService.getAccountBalance(accountId)).thenReturn(new BigDecimal("123.45"));

		BigDecimal balance = accountService.getAccountBalanceForAuthenticatedUser(accountId);

		assertEquals(new BigDecimal("123.45"), balance);
		verify(ledgerLineService).getAccountBalance(accountId);
	}
}

