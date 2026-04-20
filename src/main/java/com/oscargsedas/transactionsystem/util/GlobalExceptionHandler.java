package com.oscargsedas.transactionsystem.util;

import com.oscargsedas.transactionsystem.dto.ApiErrorResponse;
import com.oscargsedas.transactionsystem.exception.CompletedIdempotencyKeyException;
import com.oscargsedas.transactionsystem.exception.ForbiddenAccessException;
import com.oscargsedas.transactionsystem.exception.ResourceNotFoundException;
import com.oscargsedas.transactionsystem.exception.TransactionRetriesExhaustedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(
				Instant.now(),
				HttpStatus.NOT_FOUND.value(),
				"Not Found",
				ex.getMessage(),
				request.getRequestURI(),
				null
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

	@ExceptionHandler(ForbiddenAccessException.class)
	public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenAccessException ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(
				Instant.now(),
				HttpStatus.FORBIDDEN.value(),
				"Forbidden",
				ex.getMessage(),
				request.getRequestURI(),
				null
		);
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(
				Instant.now(),
				HttpStatus.BAD_REQUEST.value(),
				"Bad Request",
				ex.getMessage(),
				request.getRequestURI(),
				null
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(
				Instant.now(),
				HttpStatus.UNPROCESSABLE_ENTITY.value(),
				"Unprocessable Entity",
				ex.getMessage(),
				request.getRequestURI(),
				null
		);
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
	}

	@ExceptionHandler(CompletedIdempotencyKeyException.class)
	public ResponseEntity<ApiErrorResponse> handleCompletedIdempotency(CompletedIdempotencyKeyException ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(
				Instant.now(),
				HttpStatus.CONFLICT.value(),
				"Conflict",
				ex.getMessage(),
				request.getRequestURI(),
				null
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}

	@ExceptionHandler(TransactionRetriesExhaustedException.class)
	public ResponseEntity<ApiErrorResponse> handleRetriesExhausted(TransactionRetriesExhaustedException ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(
				Instant.now(),
				HttpStatus.SERVICE_UNAVAILABLE.value(),
				"Service Unavailable",
				ex.getMessage(),
				request.getRequestURI(),
				null
		);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(
				Instant.now(),
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"Internal Server Error",
				"An unexpected error occurred",
				request.getRequestURI(),
				null
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}
}
