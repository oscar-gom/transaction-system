package com.oscargsedas.transactionsystem.util;

import com.oscargsedas.transactionsystem.entity.User;
import com.oscargsedas.transactionsystem.exception.ForbiddenAccessException;
import com.oscargsedas.transactionsystem.exception.ResourceNotFoundException;
import com.oscargsedas.transactionsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserUtil {
	private final UserRepository userRepository;

	public User getAuthenticatedUser() {
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

	public UUID getAuthenticatedUserId() {
		return getAuthenticatedUser().getId();
	}
}

