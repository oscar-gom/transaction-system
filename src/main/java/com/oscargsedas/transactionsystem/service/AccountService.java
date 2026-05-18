package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.AccountDto;
import com.oscargsedas.transactionsystem.dto.AccountRequest;
import com.oscargsedas.transactionsystem.dto.EntityDtoMapper;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.User;
import com.oscargsedas.transactionsystem.exception.ForbiddenAccessException;
import com.oscargsedas.transactionsystem.exception.ResourceNotFoundException;
import com.oscargsedas.transactionsystem.repository.AccountRepository;
import com.oscargsedas.transactionsystem.util.AuthenticatedUserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
	public static final int PAGE_SIZE = 10;
	private final AccountRepository accountRepository;
	private final EntityDtoMapper entityDtoMapper;
	private final LedgerLineService ledgerLineService;
	private final WelcomeBonusTreasuryService welcomeBonusTreasuryService;
	private final AuthenticatedUserUtil authenticatedUserUtil;

	@Transactional
	public AccountDto createAccount(AccountRequest request) {
		User authenticatedUser = authenticatedUserUtil.getAuthenticatedUser();
		long existing = accountRepository.countByUserId(authenticatedUser.getId());
		if (existing > 0) {
			throw new ForbiddenAccessException("Each user can only have one account");
		}

		Account savedAccount = accountRepository.save(buildAccount(authenticatedUser, request.currency(), request.accountName()));


		welcomeBonusTreasuryService.applyWelcomeBonus(savedAccount);

		return entityDtoMapper.toAccountDto(savedAccount);
	}

	public AccountDto getAuthenticatedUserAccount() {
		User authenticatedUser = authenticatedUserUtil.getAuthenticatedUser();
		Account account = accountRepository.findByUserId(authenticatedUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Account not found for authenticated user"));
		return entityDtoMapper.toAccountDto(account);
	}

	public BigDecimal getAuthenticatedUserAccountBalance() {
		User authenticatedUser = authenticatedUserUtil.getAuthenticatedUser();
		Account account = accountRepository.findByUserId(authenticatedUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Account not found for authenticated user"));
		return ledgerLineService.getAccountBalance(account.getId());
	}

	private Account buildAccount(User authenticatedUser, String currency, String accountName) {
		Account account = new Account();
		account.setUser(authenticatedUser);
		account.setCurrency(currency);
		account.setAccountName(accountName);
		return account;
	}


	public Page<AccountDto> getAllAccountsForAuthenticatedUser(Pageable pageable) {
		if (pageable == null) {
			pageable = PageRequest.of(0, PAGE_SIZE);
		}
		if (pageable.getPageNumber() < 0) {
			throw new IllegalArgumentException("Page index must be zero or positive");
		}

		Pageable normalizedPageable = PageRequest.of(pageable.getPageNumber(), PAGE_SIZE, pageable.getSort());
		User authenticatedUser = authenticatedUserUtil.getAuthenticatedUser();
		return accountRepository.findAllByUserId(authenticatedUser.getId(), normalizedPageable)
				.map(entityDtoMapper::toAccountDto);
	}

	public Account getAccountEntityById(UUID accountId) {
		User authenticatedUser = authenticatedUserUtil.getAuthenticatedUser();
		Account account = findAccountOrThrow(accountId);
		validateOwnershipOrThrow(authenticatedUser.getId(), account);
		return account;
	}

	public AccountDto getAccountByAccountName(String accountName) {
		Account account = accountRepository.findByAccountName(accountName)
				.orElseThrow(() -> new ResourceNotFoundException("Account not found with name: " + accountName));

		return entityDtoMapper.toAccountDto(account);
	}

	Account getAnyAccountEntityById(UUID accountId) {
		return findAccountOrThrow(accountId);
	}

	public Page<AccountDto> searchAccountByName(String accountName, Pageable pageable) {
		if (accountName == null) {
			throw new IllegalArgumentException("Search query must be at least 5 characters long");
		}

		String normalized = accountName.trim();
		if (normalized.length() < 5) {
			throw new IllegalArgumentException("Search query must be at least 5 characters long");
		}

		if (pageable == null) {
			pageable = PageRequest.of(0, PAGE_SIZE, Sort.by("accountName"));
		}

		int requestedSize = pageable.getPageSize() <= 0 ? PAGE_SIZE : pageable.getPageSize();
		int safeSize = Math.min(requestedSize, PAGE_SIZE);

		Pageable normalizedPageable = PageRequest.of(Math.max(0, pageable.getPageNumber()), safeSize, pageable.getSort());

		Page<Account> accounts = accountRepository.findByAccountNameContainingIgnoreCase(normalized, normalizedPageable);

		return accounts.map(entityDtoMapper::toAccountDto);
	}

	public void updateAccount(UUID accountId, AccountRequest request) {
		User authenticatedUser = authenticatedUserUtil.getAuthenticatedUser();
		Account account = findAccountOrThrow(accountId);
		validateOwnershipOrThrow(authenticatedUser.getId(), account);
		account.setCurrency(request.currency());
		accountRepository.save(account);
	}

	public void deleteAccount(UUID accountId) {
		User authenticatedUser = authenticatedUserUtil.getAuthenticatedUser();
		Account account = findAccountOrThrow(accountId);
		validateOwnershipOrThrow(authenticatedUser.getId(), account);
		BigDecimal balance = ledgerLineService.getAccountBalance(accountId);

		if (balance.compareTo(BigDecimal.ZERO) != 0) {
			throw new ForbiddenAccessException("You cannot delete an account with a non-zero balance");
		}

		accountRepository.delete(account);
	}

	private Account findAccountOrThrow(UUID accountId) {
		return accountRepository.findById(accountId)
				.orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
	}

	private void validateOwnershipOrThrow(UUID authenticatedUserId, Account account) {
		if (account.getUser() == null || account.getUser().getId() == null || !account.getUser().getId().equals(authenticatedUserId)) {
			throw new ForbiddenAccessException("You do not have permission to access this account");
		}
	}
}
