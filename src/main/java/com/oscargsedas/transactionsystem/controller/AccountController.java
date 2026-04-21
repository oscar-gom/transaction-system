package com.oscargsedas.transactionsystem.controller;

import com.oscargsedas.transactionsystem.dto.AccountDto;
import com.oscargsedas.transactionsystem.dto.AccountRequest;
import com.oscargsedas.transactionsystem.dto.ApiSuccessResponse;
import com.oscargsedas.transactionsystem.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
	private final AccountService accountService;

	@GetMapping("/{id}")
	public ResponseEntity<ApiSuccessResponse<AccountDto>> getAccountById(
			@PathVariable UUID id,
			HttpServletRequest request) {
		AccountDto accountDto = accountService.getAccountById(id);

		ApiSuccessResponse<AccountDto> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Account retrieved successfully",
				request.getRequestURI(),
				accountDto
		);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/create")
	public ResponseEntity<ApiSuccessResponse<Void>> createAccount(
			@RequestBody AccountRequest accountRequest,
			HttpServletRequest request) {
		accountService.createAccount(accountRequest);

		ApiSuccessResponse<Void> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.CREATED.value(),
				"Account created successfully",
				request.getRequestURI(),
				null
		);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/update/{id}")
	public ResponseEntity<ApiSuccessResponse<Void>> updateAccount(
			@PathVariable UUID id,
			@RequestBody AccountRequest accountRequest,
			HttpServletRequest request) {
		accountService.updateAccount(id, accountRequest);

		ApiSuccessResponse<Void> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Account updated successfully",
				request.getRequestURI(),
				null
		);

		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/delete/{id}")
	public ResponseEntity<ApiSuccessResponse<Void>> deleteAccount(
			@PathVariable UUID id,
			HttpServletRequest request) {
		accountService.deleteAccount(id);

		ApiSuccessResponse<Void> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Account deleted successfully",
				request.getRequestURI(),
				null
		);

		return ResponseEntity.ok(response);
	}
}
