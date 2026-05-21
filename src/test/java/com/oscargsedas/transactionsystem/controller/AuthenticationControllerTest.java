package com.oscargsedas.transactionsystem.controller;

import com.oscargsedas.transactionsystem.dto.AuthResponse;
import com.oscargsedas.transactionsystem.dto.UserRequest;
import com.oscargsedas.transactionsystem.entity.User;
import com.oscargsedas.transactionsystem.repository.UserRepository;
import com.oscargsedas.transactionsystem.security.JwtUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

	private static Validator validator;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtUtil jwtUtil;

	@InjectMocks
	private AuthenticationController authenticationController;

	@BeforeAll
	static void initValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void registerRejectsPasswordTooShort() {
		UserRequest request = new UserRequest("user@example.com", "User", "Example", "Abc1234");

		Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);

		assertFalse(violations.isEmpty());
	}

	@Test
	void registerRejectsPasswordWithoutUppercase() {
		UserRequest request = new UserRequest("user@example.com", "User", "Example", "password1");

		Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);

		assertFalse(violations.isEmpty());
	}

	@Test
	void registerRejectsPasswordWithoutNumber() {
		UserRequest request = new UserRequest("user@example.com", "User", "Example", "Password");

		Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);

		assertFalse(violations.isEmpty());
	}

	@Test
	void registerAcceptsValidPassword() {
		UserRequest request = new UserRequest("user@example.com", "User", "Example", "Password1");
		UUID userId = UUID.randomUUID();
		User savedUser = new User();
		savedUser.setId(userId);

		Set<ConstraintViolation<UserRequest>> violations = validator.validate(request);
		assertTrue(violations.isEmpty());

		when(userRepository.existsByEmail(eq("user@example.com"))).thenReturn(false);
		when(passwordEncoder.encode(eq("Password1"))).thenReturn("encoded");
		when(userRepository.save(any(User.class))).thenReturn(savedUser);

		ResponseEntity<AuthResponse> response = authenticationController.registerUser(request);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertTrue(response.getBody().isSuccess());
		assertEquals(userId, response.getBody().getUserId());
	}

	@Test
	void loginAcceptsValidPassword() {
		UserRequest request = new UserRequest("user@example.com", "User", "Example", "Password1");
		Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
		UserDetails userDetails = org.springframework.security.core.userdetails.User
				.withUsername("user@example.com")
				.password("encoded")
				.authorities("USER")
				.build();
		User user = new User();
		UUID userId = UUID.randomUUID();
		user.setId(userId);

		when(authenticationManager.authenticate(any())).thenReturn(authentication);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(authentication.getName()).thenReturn("user@example.com");
		when(jwtUtil.generateToken(eq("user@example.com"))).thenReturn("token");
		when(userRepository.findByEmail(eq("user@example.com"))).thenReturn(user);

		ResponseEntity<AuthResponse> response = authenticationController.authenticateUser(request);

		assertEquals(200, response.getStatusCode().value());
		assertNotNull(response.getBody());
		assertTrue(response.getBody().isSuccess());
		assertEquals("token", response.getBody().getToken());
		assertEquals(userId, response.getBody().getUserId());
	}
}
