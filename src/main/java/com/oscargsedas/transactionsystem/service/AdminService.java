package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.entity.AccountType;
import com.oscargsedas.transactionsystem.exception.ForbiddenAccessException;
import com.oscargsedas.transactionsystem.repository.UserRepository;
import com.oscargsedas.transactionsystem.util.AuthenticatedUserUtil;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {
	private final UserRepository userRepository;
	private final AuthenticatedUserUtil authenticatedUserUtil;

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
		userRepository.save(user);
	}
}
