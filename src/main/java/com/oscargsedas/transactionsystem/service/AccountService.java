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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
	public static final int PAGE_SIZE = 10;
	private final AccountRepository accountRepository;
	private final UserRepository userRepository;
	private final TransactionRepository transactionRepository;
	private final EntityDtoMapper entityDtoMapper;
	private final LedgerLineService ledgerLineService;
	private final SystemTreasuryAccountService systemTreasuryAccountService;
	private final WelcomeBonusProperties welcomeBonusProperties;

	@Transactional
	public AccountDto createAccount(AccountRequest request) {
		User authenticatedUser = getAuthenticatedUser();
		long existing = accountRepository.countByUserId(authenticatedUser.getId());
		if (existing > 0) {
			throw new ForbiddenAccessException("Each user can only have one account");
		}

		Account savedAccount = accountRepository.save(buildAccount(authenticatedUser, request.currency()));


		applyWelcomeBonus(savedAccount);

		return entityDtoMapper.toAccountDto(savedAccount);
	}

	public AccountDto getAuthenticatedUserAccount() {
		User authenticatedUser = getAuthenticatedUser();
		Account account = accountRepository.findByUserId(authenticatedUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Account not found for authenticated user"));
		return entityDtoMapper.toAccountDto(account);
	}

	public BigDecimal getAuthenticatedUserAccountBalance() {
		User authenticatedUser = getAuthenticatedUser();
		Account account = accountRepository.findByUserId(authenticatedUser.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Account not found for authenticated user"));
		return ledgerLineService.getAccountBalance(account.getId());
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

	public Page<AccountDto> getAllAccountsForAuthenticatedUser(Pageable pageable) {
		if (pageable == null) {
			pageable = PageRequest.of(0, PAGE_SIZE);
		}
		if (pageable.getPageNumber() < 0) {
			throw new IllegalArgumentException("Page index must be zero or positive");
		}

		Pageable normalizedPageable = PageRequest.of(pageable.getPageNumber(), PAGE_SIZE, pageable.getSort());
		User authenticatedUser = getAuthenticatedUser();
		return accountRepository.findAllByUserId(authenticatedUser.getId(), normalizedPageable)
				.map(entityDtoMapper::toAccountDto);
	}

	public Account getAccountEntityById(UUID accountId) {
		User authenticatedUser = getAuthenticatedUser();
		Account account = findAccountOrThrow(accountId);
		validateOwnershipOrThrow(authenticatedUser.getId(), account);
		return account;
	}


	Account getAnyAccountEntityById(UUID accountId) {
		return findAccountOrThrow(accountId);
	}

	UUID getAuthenticatedUserId() {
		return getAuthenticatedUser().getId();
	}

	public void updateAccount(UUID accountId, AccountRequest request) {
		User authenticatedUser = getAuthenticatedUser();
		Account account = findAccountOrThrow(accountId);
		validateOwnershipOrThrow(authenticatedUser.getId(), account);
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
