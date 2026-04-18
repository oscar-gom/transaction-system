package com.oscargsedas.transactionsystem.controller;

import com.oscargsedas.transactionsystem.dto.AuthResponse;
import com.oscargsedas.transactionsystem.dto.UserRequest;
import com.oscargsedas.transactionsystem.entity.User;
import com.oscargsedas.transactionsystem.repository.UserRepository;
import com.oscargsedas.transactionsystem.security.JwtUtil;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {
	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final PasswordEncoder encoder;
	private final JwtUtil jwtUtils;

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> authenticateUser(@RequestBody UserRequest userRequest) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						userRequest.getEmail(),
						userRequest.getPassword()
				)
		);

		final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
		String token = jwtUtils.generateToken(userDetails.getUsername());
		return ResponseEntity.ok(new AuthResponse("Login successful", token, true));
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> registerUser(@RequestBody UserRequest userRequest) {
		if (userRepository.existsByEmail(userRequest.getEmail())) {
			return ResponseEntity.badRequest().body(new AuthResponse("Email is already in use!", null, false));
		}

		final User newUser = new User();
		newUser.setEmail(userRequest.getEmail());
		newUser.setName(userRequest.getName());
		newUser.setSurname(userRequest.getSurname());
		newUser.setPassword(encoder.encode(userRequest.getPassword()));

		userRepository.save(newUser);
		return ResponseEntity.ok(new AuthResponse("User registered successfully!", null, true));
	}

}
