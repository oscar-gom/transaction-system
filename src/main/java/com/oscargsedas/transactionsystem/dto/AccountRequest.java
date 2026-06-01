package com.oscargsedas.transactionsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record AccountRequest(
		@NotBlank(message = "accountName is mandatory")
		@Size(min = 5, max = 100, message = "accountName must be between 5 and 100 characters")
		String accountName,

		@NotBlank(message = "currency is mandatory")
		@Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a valid ISO code")
		String currency
) {}
