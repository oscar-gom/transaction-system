package com.oscargsedas.transactionsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record AccountRequest(
		@NotNull
		UUID userId,

		@NotBlank(message = "currency is mandatory")
		@Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a valid ISO code")
		String currency
) {}
