package com.oscargsedas.transactionsystem.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionRequest(
		@NotNull
		UUID senderId,
		@NotNull
		UUID receiverId,
		@NotNull
		UUID idempotencyKey,
		@NotNull
		@Digits(integer = 15, fraction = 2, message = "amount must be a valid number with up to 15 digits and 2 decimal places")
		@Positive(message = "amount must be greater than zero")
		@DecimalMax(value = "1000000000.00", message = "amount must not exceed 1,000,000,000.00")
		BigDecimal amount
) {
}
