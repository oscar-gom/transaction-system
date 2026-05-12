package com.oscargsedas.transactionsystem.controller;

import com.oscargsedas.transactionsystem.dto.AccountDto;
import com.oscargsedas.transactionsystem.dto.AccountRequest;
import com.oscargsedas.transactionsystem.dto.ApiSuccessResponse;
import com.oscargsedas.transactionsystem.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v3/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Endpoints to manage user accounts and balances")
public class AccountController {
	private final AccountService accountService;

	@GetMapping("/all")
	@Operation(summary = "List accounts", description = "List paginated accounts for the authenticated user")
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

	@GetMapping("/me")
	@Operation(summary = "Get my account", description = "Return the account of the currently authenticated user")
	public ResponseEntity<ApiSuccessResponse<AccountDto>> getMyAccount(HttpServletRequest request) {
		AccountDto accountDto = accountService.getAuthenticatedUserAccount();

		ApiSuccessResponse<AccountDto> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Authenticated user's account retrieved successfully",
				request.getRequestURI(),
				accountDto
		);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/me/balance")
	@Operation(summary = "Get my account balance", description = "Return the balance of the currently authenticated user's account")
	public ResponseEntity<ApiSuccessResponse<BigDecimal>> getMyAccountBalance(HttpServletRequest request) {
		BigDecimal balance = accountService.getAuthenticatedUserAccountBalance();

		ApiSuccessResponse<BigDecimal> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Authenticated user's account balance retrieved successfully",
				request.getRequestURI(),
				balance
		);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/{name}")
	@Operation(summary = "Get account by name", description = "Return the account with the specified name")
	public ResponseEntity<ApiSuccessResponse<AccountDto>> getAccountByName(HttpServletRequest request, @PathVariable String name) {
		AccountDto accountDto = accountService.getAccountByAccountName(name);

		ApiSuccessResponse<AccountDto> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Account retrieved successfully",
				request.getRequestURI(),
				accountDto
		);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/search")
	@Operation(summary = "Search accounts by name", description = "Search accounts by name containing the specified string. Returns paginated results.")
	public ResponseEntity<ApiSuccessResponse<Page<AccountDto>>> searchAccountsByName(
			HttpServletRequest request,
			@RequestParam(name = "q") String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		Pageable pageable = PageRequest.of(page, Math.clamp(size, 1, AccountService.PAGE_SIZE));
		Page<AccountDto> accounts = accountService.searchAccountByName(q, pageable);

		ApiSuccessResponse<Page<AccountDto>> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Accounts search results",
				request.getRequestURI(),
				accounts
		);

		return ResponseEntity.ok(response);
	}


	@PostMapping("/create")
	@Operation(summary = "Create account", description = "Create an account for the authenticated user. Each user may have only one account.")
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
