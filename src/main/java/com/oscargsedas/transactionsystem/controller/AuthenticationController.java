package com.oscargsedas.transactionsystem.controller;

import com.oscargsedas.transactionsystem.dto.ApiSuccessResponse;
import com.oscargsedas.transactionsystem.dto.AuthResponse;
import com.oscargsedas.transactionsystem.dto.UserRequest;
import com.oscargsedas.transactionsystem.entity.User;
import com.oscargsedas.transactionsystem.repository.UserRepository;
import com.oscargsedas.transactionsystem.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v4/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login endpoints")
public class AuthenticationController {
	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final PasswordEncoder encoder;
	private final JwtUtil jwtUtils;

	@PostMapping("/login")
	@Operation(summary = "Login", description = "Authenticate a user and return a JWT token and user id")
	public ResponseEntity<ApiSuccessResponse<AuthResponse>> authenticateUser(@Valid @RequestBody UserRequest userRequest, HttpServletRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						userRequest.getEmail(),
						userRequest.getPassword()
				)
		);

		final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
		String username = authentication.getName();
		String token = jwtUtils.generateToken(username);
		User authenticatedUser = userRepository.findByEmail(username);
		AuthResponse authData = new AuthResponse("Login successful", token, true, authenticatedUser != null ? authenticatedUser.getId() : null);

		ApiSuccessResponse<AuthResponse> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Login successful",
				request.getRequestURI(),
				authData
		);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/register")
	@Operation(summary = "Register", description = "Register a new user and return the new user's id")
	public ResponseEntity<ApiSuccessResponse<AuthResponse>> registerUser(@Valid @RequestBody UserRequest userRequest, HttpServletRequest request) {
		if (userRepository.existsByEmail(userRequest.getEmail())) {
			AuthResponse authData = new AuthResponse("Email is already in use!", null, false, null);
			ApiSuccessResponse<AuthResponse> response = new ApiSuccessResponse<>(
					Instant.now(),
					HttpStatus.BAD_REQUEST.value(),
					"Email is already in use!",
					request.getRequestURI(),
					authData
			);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		final User newUser = new User();
		newUser.setEmail(userRequest.getEmail());
		newUser.setName(userRequest.getName());
		newUser.setSurname(userRequest.getSurname());
		newUser.setPassword(encoder.encode(userRequest.getPassword()));

		User savedUser = userRepository.save(newUser);
		AuthResponse authData = new AuthResponse("User registered successfully!", null, true, savedUser.getId());
		ApiSuccessResponse<AuthResponse> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"User registered successfully!",
				request.getRequestURI(),
				authData
		);

		return ResponseEntity.ok(response);
	}

}
