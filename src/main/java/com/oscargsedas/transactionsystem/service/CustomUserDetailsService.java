package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.entity.User;
import com.oscargsedas.transactionsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
		User user = userRepository.findByEmail(email);

		if (user == null) {
			throw new UsernameNotFoundException("User not found with email: " + email);
		}

		return new com.oscargsedas.transactionsystem.security.CustomUserDetails(
				user.getEmail(),
				user.getPassword(),
				Collections.emptyList(),
				user.getTokenVersion()
		);
	}
}
