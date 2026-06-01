package com.oscargsedas.transactionsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ContactRequest(
		@NotNull
		UUID contactAccountId,
		@NotBlank(message = "alias is mandatory")
		@Size(min = 1, max = 60, message = "alias must be between 1 and 60 characters")
		String alias,
		@Size(max = 255, message = "note must be at most 255 characters")
		String note,
		boolean favorite
) {
}
