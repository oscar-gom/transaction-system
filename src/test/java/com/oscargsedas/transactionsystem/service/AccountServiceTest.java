package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.AccountRequest;
import com.oscargsedas.transactionsystem.dto.EntityDtoMapper;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.User;
import com.oscargsedas.transactionsystem.exception.ForbiddenAccessException;
import com.oscargsedas.transactionsystem.repository.AccountRepository;
import com.oscargsedas.transactionsystem.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private EntityDtoMapper entityDtoMapper;

	@Mock
	private LedgerLineService ledgerLineService;

	@Mock
	private WelcomeBonusTreasuryService welcomeBonusTreasuryService;

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
		when(entityDtoMapper.toAccountDto(any(Account.class))).thenReturn(null);

		accountService.createAccount(new AccountRequest("EUR", "test-account"));

		verify(welcomeBonusTreasuryService).applyWelcomeBonus(any(Account.class));
	}

	@Test
	void createAccountRejectsSecondAccountForSameUser() {
		UUID userId = UUID.randomUUID();
		User authenticatedUser = new User();
		authenticatedUser.setId(userId);
		authenticatedUser.setEmail("user@example.com");

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("user@example.com", "n/a")
		);

		when(userRepository.findByEmail("user@example.com")).thenReturn(authenticatedUser);
		when(accountRepository.countByUserId(userId)).thenReturn(1L);

		assertThrows(ForbiddenAccessException.class, () -> accountService.createAccount(new AccountRequest("test-account", "EUR")));

		verify(welcomeBonusTreasuryService, never()).applyWelcomeBonus(any(Account.class));
		verify(accountRepository, never()).save(any(Account.class));
	}

	@Test
	void getAuthenticatedUserAccountBalanceReturnsBalanceForCurrentUserAccount() {
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
		when(accountRepository.findByUserId(userId)).thenReturn(java.util.Optional.of(account));
		when(ledgerLineService.getAccountBalance(accountId)).thenReturn(new BigDecimal("123.45"));

		BigDecimal balance = accountService.getAuthenticatedUserAccountBalance();

		assertEquals(new BigDecimal("123.45"), balance);
		verify(ledgerLineService).getAccountBalance(accountId);
	}

	@Test
	void getAccountByAccountNameReturnsMappedDto() {
		UUID accountId = UUID.randomUUID();
		User owner = new User();
		owner.setId(UUID.randomUUID());
		owner.setEmail("owner@example.com");

		Account account = new Account();
		account.setId(accountId);
		account.setUser(owner);
		account.setAccountName("my-account");
		account.setCurrency("EUR");

		com.oscargsedas.transactionsystem.dto.AccountDto accountDto = mock(com.oscargsedas.transactionsystem.dto.AccountDto.class);

		when(accountRepository.findByAccountName("my-account")).thenReturn(Optional.of(account));
		when(entityDtoMapper.toAccountDto(account)).thenReturn(accountDto);

		com.oscargsedas.transactionsystem.dto.AccountDto result = accountService.getAccountByAccountName("my-account");

		assertEquals(accountDto, result);
	}

	@Test
	void getAccountEntityByIdReturnsAccountForOwner() {
		UUID userId = UUID.randomUUID();
		UUID accountId = UUID.randomUUID();
		User authenticatedUser = new User();
		authenticatedUser.setId(userId);
		authenticatedUser.setEmail("owner2@example.com");

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("owner2@example.com", "n/a")
		);

		Account account = new Account();
		account.setId(accountId);
		account.setUser(authenticatedUser);
		account.setCurrency("EUR");

		when(userRepository.findByEmail("owner2@example.com")).thenReturn(authenticatedUser);
		when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

		Account result = accountService.getAccountEntityById(accountId);

		assertEquals(account, result);
	}
}

