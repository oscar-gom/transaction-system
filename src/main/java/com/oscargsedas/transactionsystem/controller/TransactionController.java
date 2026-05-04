package com.oscargsedas.transactionsystem.controller;

import com.oscargsedas.transactionsystem.dto.ApiSuccessResponse;
import com.oscargsedas.transactionsystem.dto.TransactionDto;
import com.oscargsedas.transactionsystem.dto.TransactionRequest;
import com.oscargsedas.transactionsystem.service.TransactionService;
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

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Endpoints to create and view transactions")
public class TransactionController {
	private final TransactionService transactionService;

	@GetMapping("/all")
	@Operation(summary = "List transactions", description = "List paginated transactions where the authenticated user is either sender or receiver")
	public ResponseEntity<ApiSuccessResponse<Page<TransactionDto>>> getAllTransactions(
			@RequestParam(defaultValue = "0") int page,
			HttpServletRequest request) {
		Pageable pageable = PageRequest.of(page, TransactionService.PAGE_SIZE);
		Page<TransactionDto> transactions = transactionService.getTransactionsForAuthenticatedUser(pageable);

		ApiSuccessResponse<Page<TransactionDto>> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Transactions retrieved successfully",
				request.getRequestURI(),
				transactions
		);

		return ResponseEntity.ok(response);

	}

	@GetMapping("/{id}")
	@Operation(summary = "Get transaction", description = "Retrieve a transaction by id. Only participants (sender or receiver) can access it.")
	public ResponseEntity<ApiSuccessResponse<TransactionDto>> getTransactionById(
			@PathVariable UUID id,
			HttpServletRequest request
	) {
		TransactionDto transactionDto = transactionService.getTransactionById(id);

		ApiSuccessResponse<TransactionDto> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Transaction retrieved successfully",
				request.getRequestURI(),
				transactionDto
		);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/create")
	@Operation(summary = "Create transaction", description = "Create a transaction between accounts. The authenticated user must be the sender.")
	public ResponseEntity<ApiSuccessResponse<TransactionDto>> createTransaction(
			@Valid @RequestBody TransactionRequest transactionRequest,
			HttpServletRequest request
	) {
		TransactionDto transactionDto = transactionService.createTransaction(transactionRequest);

		ApiSuccessResponse<TransactionDto> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.CREATED.value(),
				"Transaction created successfully",
				request.getRequestURI(),
				transactionDto
		);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
