package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.AccountDto;
import com.oscargsedas.transactionsystem.dto.AccountRequest;
import com.oscargsedas.transactionsystem.dto.EntityDtoMapper;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.Transaction;
import com.oscargsedas.transactionsystem.entity.TransactionStatus;
import com.oscargsedas.transactionsystem.entity.User;
import com.oscargsedas.transactionsystem.exception.ForbiddenAccessException;
import com.oscargsedas.transactionsystem.exception.ResourceNotFoundException;
import com.oscargsedas.transactionsystem.repository.AccountRepository;
import com.oscargsedas.transactionsystem.repository.TransactionRepository;
import com.oscargsedas.transactionsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
	private final AccountRepository accountRepository;
	private final UserRepository userRepository;
	private final TransactionRepository transactionRepository;
	private final EntityDtoMapper entityDtoMapper;
	private final LedgerLineService ledgerLineService;
	private final SystemTreasuryAccountService systemTreasuryAccountService;
	private final WelcomeBonusProperties welcomeBonusProperties;

	@Transactional
	public void createAccount(AccountRequest request) {
		User authenticatedUser = getAuthenticatedUser();
		assertSameUser(authenticatedUser.getId(), request.userId(), "You cannot create an account for another user");

		boolean isFirstAccount = accountRepository.countByUserId(authenticatedUser.getId()) == 0;
		Account savedAccount = accountRepository.save(buildAccount(authenticatedUser, request.currency()));

		if (isFirstAccount) {
			applyWelcomeBonus(savedAccount);
		}
	}

	private void applyWelcomeBonus(Account receiverAccount) {
		Account treasuryAccount = systemTreasuryAccountService.getOrCreateTreasuryAccount(receiverAccount.getCurrency());
		Transaction savedTransaction = transactionRepository.save(buildWelcomeTransaction(treasuryAccount, receiverAccount));
		ledgerLineService.createLedgerLinesForTransaction(savedTransaction);
	}

	private Account buildAccount(User authenticatedUser, String currency) {
		Account account = new Account();
		account.setUser(authenticatedUser);
		account.setCurrency(currency);
		return account;
	}

	private Transaction buildWelcomeTransaction(Account senderAccount, Account receiverAccount) {
		Transaction welcomeTransaction = new Transaction();
		welcomeTransaction.setSenderAccount(senderAccount);
		welcomeTransaction.setReceiverAccount(receiverAccount);
		welcomeTransaction.setIdempotencyKey(UUID.randomUUID());
		welcomeTransaction.setAmount(welcomeBonusProperties.getAmount());
		welcomeTransaction.setStatus(TransactionStatus.COMPLETED);
		return welcomeTransaction;
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

	public UUID getAuthenticatedUserId() {
		return getAuthenticatedUser().getId();
	}

	public void updateAccount(UUID accountId, AccountRequest request) {
		User authenticatedUser = getAuthenticatedUser();
		Account account = findAccountOrThrow(accountId);
		validateOwnershipOrThrow(authenticatedUser.getId(), account);
		assertSameUser(authenticatedUser.getId(), request.userId(), "You cannot change the owner of the account");

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

	private void assertSameUser(UUID authenticatedUserId, UUID requestedUserId, String message) {
		if (!authenticatedUserId.equals(requestedUserId)) {
			throw new ForbiddenAccessException(message);
		}
	}
}
