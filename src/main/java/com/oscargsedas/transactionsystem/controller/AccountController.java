package com.oscargsedas.transactionsystem.controller;

import com.oscargsedas.transactionsystem.dto.AccountDto;
import com.oscargsedas.transactionsystem.dto.AccountRequest;
import com.oscargsedas.transactionsystem.dto.ApiSuccessResponse;
import com.oscargsedas.transactionsystem.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
	private final AccountService accountService;

	@GetMapping("/all")
	public ResponseEntity<ApiSuccessResponse<Page<AccountDto>>> getAllAccounts(
			@RequestParam(defaultValue = "0") int page,
			HttpServletRequest request) {
		Pageable pageable = PageRequest.of(page, AccountService.PAGE_SIZE);
		Page<AccountDto> accounts = accountService.getAllAccountsForAuthenticatedUser(pageable);

		ApiSuccessResponse<Page<AccountDto>> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Accounts retrieved successfully",
				request.getRequestURI(),
				accounts
		);

		return ResponseEntity.ok(response);
	}

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

	@GetMapping("/{id}/balance")
	public ResponseEntity<ApiSuccessResponse<BigDecimal>> getAccountBalance(
			@PathVariable UUID id,
			HttpServletRequest request) {
		BigDecimal balance = accountService.getAccountBalanceForAuthenticatedUser(id);

		ApiSuccessResponse<BigDecimal> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Account balance retrieved successfully",
				request.getRequestURI(),
				balance
		);

		return ResponseEntity.ok(response);
	}


	@PostMapping("/create")
	public ResponseEntity<ApiSuccessResponse<AccountDto>> createAccount(
			@Valid @RequestBody AccountRequest accountRequest,
			HttpServletRequest request) {
		AccountDto accountDto = accountService.createAccount(accountRequest);

		ApiSuccessResponse<AccountDto> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.CREATED.value(),
				"Account created successfully",
				request.getRequestURI(),
				accountDto
		);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
