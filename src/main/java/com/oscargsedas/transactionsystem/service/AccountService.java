package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.AccountDto;
import com.oscargsedas.transactionsystem.dto.AccountRequest;
import com.oscargsedas.transactionsystem.dto.EntityDtoMapper;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.User;
import com.oscargsedas.transactionsystem.exception.ForbiddenAccessException;
import com.oscargsedas.transactionsystem.exception.ResourceNotFoundException;
import com.oscargsedas.transactionsystem.repository.AccountRepository;
import com.oscargsedas.transactionsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
	private final AccountRepository accountRepository;
	private final UserRepository userRepository;
	private final EntityDtoMapper entityDtoMapper;
	private final LedgerLineService ledgerLineService;

	public void createAccount(AccountRequest request) {
		User authenticatedUser = getAuthenticatedUser();

		if (!authenticatedUser.getId().equals(request.userId())) {
			throw new ForbiddenAccessException("You cannot create an account for another user");
		}

		Account account = new Account();
		account.setUser(authenticatedUser);
		account.setCurrency(request.currency());

		accountRepository.save(account);
	}

	public AccountDto getAccountById(UUID accountId) {
		User authenticatedUser = getAuthenticatedUser();
		Account account = findAccountOrThrow(accountId);
		validateOwnershipOrThrow(authenticatedUser.getId(), account);
		return entityDtoMapper.toAccountDto(account);
	}

	public Account getAccountEntityById(UUID accountId) {
		User authenticatedUser = getAuthenticatedUser();
		Account account = findAccountOrThrow(accountId);
		validateOwnershipOrThrow(authenticatedUser.getId(), account);
		return account;
	}

	public void updateAccount(UUID accountId, AccountRequest request) {
		User authenticatedUser = getAuthenticatedUser();
		Account account = findAccountOrThrow(accountId);
		validateOwnershipOrThrow(authenticatedUser.getId(), account);

		if (!authenticatedUser.getId().equals(request.userId())) {
			throw new ForbiddenAccessException("You cannot change the owner of the account");
		}

		account.setCurrency(request.currency());

		accountRepository.save(account);
	}

	public void deleteAccount(UUID accountId) {
		User authenticatedUser = getAuthenticatedUser();
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

	private User getAuthenticatedUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
			throw new ForbiddenAccessException("You must be authenticated to perform this action");
		}

		String email = authentication.getName();
		User user = userRepository.findByEmail(email);
		if (user == null) {
			throw new ResourceNotFoundException("Authenticated user not found with email: " + email);
		}
		return user;
	}

	private void validateOwnershipOrThrow(UUID authenticatedUserId, Account account) {
		if (account.getUser() == null || account.getUser().getId() == null || !account.getUser().getId().equals(authenticatedUserId)) {
			throw new ForbiddenAccessException("You do not have permission to access this account");
		}
	}
}
