package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.TransactionRequest;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.AccountType;
import com.oscargsedas.transactionsystem.exception.ForbiddenAccessException;
import com.oscargsedas.transactionsystem.exception.ResourceNotFoundException;
import com.oscargsedas.transactionsystem.repository.AccountRepository;
import com.oscargsedas.transactionsystem.repository.UserRepository;
import com.oscargsedas.transactionsystem.util.AuthenticatedUserUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Validated
@Service
@RequiredArgsConstructor
public class AdminService {
	private final UserRepository userRepository;
	private final AuthenticatedUserUtil authenticatedUserUtil;
	private final TreasuryService treasuryService;
	private final AccountRepository accountRepository;

	public void promoteToAdmin(@NotNull String email) {
		changeAccountTypeForUser(email, AccountType.ADMIN, "Only admins can promote users to admin");
	}

	public void demoteFromAdmin(@NotNull String email) {
		changeAccountTypeForUser(email, AccountType.USER, "Only admins can demote users from admin");
	}

	private void changeAccountTypeForUser(String email, AccountType targetType, String forbiddenMessage) {
		if (!authenticatedUserUtil.isAuthenticatedUserAdmin()) {
			throw new ForbiddenAccessException(forbiddenMessage);
		}

		if (targetType == AccountType.USER) {
			String authenticatedEmail = authenticatedUserUtil.getAuthenticatedUser().getEmail();
			if (authenticatedEmail != null && authenticatedEmail.equalsIgnoreCase(email)) {
				throw new ForbiddenAccessException("Admins cannot demote their own account");
			}
		}

		var user = userRepository.findByEmail(email);
		if (user == null) {
			throw new IllegalArgumentException("User not found");
		}

		user.setAccountType(targetType);
		user.setTokenVersion(user.getTokenVersion() + 1);
		userRepository.save(user);
	}

	public void createCompensationTransaction(@Valid TransactionRequest request) {
		if (!authenticatedUserUtil.isAuthenticatedUserAdmin()) {
			throw new ForbiddenAccessException("Only admins can create compensation transactions");
		}

		Account receiverAccount = accountRepository.findById(request.receiverId())
				.orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + request.receiverId()));

		if (receiverAccount.getUser() == null || receiverAccount.getUser().getAccountType() != AccountType.USER) {
			throw new ForbiddenAccessException("Compensation transactions are only allowed for USER accounts");
		}

		treasuryService.createCompensationTransaction(receiverAccount, request.amount(), request.idempotencyKey());
	}
}
