package com.oscargsedas.transactionsystem.dto;

import jakarta.validation.constraints.Size;

public record ContactUpdateRequest(
		@Size(min = 1, max = 60, message = "alias must be between 1 and 60 characters")
		String alias,
		@Size(max = 255, message = "note must be at most 255 characters")
		String note,
		Boolean favorite
) {
}
