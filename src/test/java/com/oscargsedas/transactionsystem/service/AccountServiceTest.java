package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.AccountRequest;
import com.oscargsedas.transactionsystem.dto.EntityDtoMapper;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.User;
import com.oscargsedas.transactionsystem.exception.ForbiddenAccessException;
import com.oscargsedas.transactionsystem.repository.AccountRepository;
import com.oscargsedas.transactionsystem.util.AuthenticatedUserUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
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
	private EntityDtoMapper entityDtoMapper;

	@Mock
	private LedgerLineService ledgerLineService;

	@Mock
	private TreasuryService treasuryService;

	@Mock
	private AuthenticatedUserUtil authenticatedUserUtil;

	@InjectMocks
	private AccountService accountService;

	@Test
	void createAccountAppliesWelcomeBonusOnFirstAccount() {
		UUID userId = UUID.randomUUID();
		User authenticatedUser = new User();
		authenticatedUser.setId(userId);
		authenticatedUser.setEmail("user@example.com");

		when(authenticatedUserUtil.getAuthenticatedUser()).thenReturn(authenticatedUser);
		when(accountRepository.countByUserId(userId)).thenReturn(0L);
		when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
			Account account = invocation.getArgument(0);
			if (account.getId() == null) {
				account.setId(UUID.randomUUID());
			}
			return account;
		});
		when(entityDtoMapper.toAccountDto(any(Account.class))).thenReturn(null);

		accountService.createAccount(new AccountRequest("test-account", "EUR"));

		verify(treasuryService).applyWelcomeBonus(any(Account.class));
	}

	@Test
	void createAccountRejectsSecondAccountForSameUser() {
		UUID userId = UUID.randomUUID();
		User authenticatedUser = new User();
		authenticatedUser.setId(userId);
		authenticatedUser.setEmail("user@example.com");

		when(authenticatedUserUtil.getAuthenticatedUser()).thenReturn(authenticatedUser);
		when(accountRepository.countByUserId(userId)).thenReturn(1L);

		assertThrows(ForbiddenAccessException.class, () -> accountService.createAccount(new AccountRequest("test-account", "EUR")));

		verify(treasuryService, never()).applyWelcomeBonus(any(Account.class));
		verify(accountRepository, never()).save(any(Account.class));
	}

	@Test
	void getAuthenticatedUserAccountBalanceReturnsBalanceForCurrentUserAccount() {
		UUID userId = UUID.randomUUID();
		UUID accountId = UUID.randomUUID();
		User authenticatedUser = new User();
		authenticatedUser.setId(userId);
		authenticatedUser.setEmail("user@example.com");

		Account account = new Account();
		account.setId(accountId);
		account.setUser(authenticatedUser);
		account.setCurrency("EUR");

		when(authenticatedUserUtil.getAuthenticatedUser()).thenReturn(authenticatedUser);
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

		Account account = new Account();
		account.setId(accountId);
		account.setUser(authenticatedUser);
		account.setCurrency("EUR");

		when(authenticatedUserUtil.getAuthenticatedUser()).thenReturn(authenticatedUser);
		when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

		Account result = accountService.getAccountEntityById(accountId);

		assertEquals(account, result);
	}

	@Test
	void searchAccountByNameReturnsPagedDtos() {
		Account a1 = new Account();
		a1.setId(UUID.randomUUID());
		a1.setAccountName("manolito1");
		a1.setCurrency("EUR");

		Account a2 = new Account();
		a2.setId(UUID.randomUUID());
		a2.setAccountName("manolito2");
		a2.setCurrency("EUR");

		com.oscargsedas.transactionsystem.dto.AccountDto d1 = mock(com.oscargsedas.transactionsystem.dto.AccountDto.class);
		com.oscargsedas.transactionsystem.dto.AccountDto d2 = mock(com.oscargsedas.transactionsystem.dto.AccountDto.class);

		Page<Account> page = new PageImpl<>(List.of(a1, a2), PageRequest.of(0, 10), 2);

		when(accountRepository.findByAccountNameContainingIgnoreCase(eq("manol"), any(PageRequest.class))).thenReturn(page);
		when(entityDtoMapper.toAccountDto(a1)).thenReturn(d1);
		when(entityDtoMapper.toAccountDto(a2)).thenReturn(d2);

		Page<com.oscargsedas.transactionsystem.dto.AccountDto> result = accountService.searchAccountByName("manol", PageRequest.of(0, 10));

		assertEquals(2, result.getTotalElements());
		assertEquals(d1, result.getContent().get(0));
		assertEquals(d2, result.getContent().get(1));
	}

	@Test
	void searchAccountByNameThrowsForShortQuery() {
		assertThrows(IllegalArgumentException.class, () -> accountService.searchAccountByName("mano", PageRequest.of(0, 10)));
	}
}
