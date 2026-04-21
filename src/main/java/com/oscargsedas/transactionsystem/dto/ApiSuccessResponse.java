package com.oscargsedas.transactionsystem.dto;

import java.time.Instant;

public record ApiSuccessResponse<T>(
		Instant timestamp,
		int status,
		String message,
		String path,
		T data
) {
}

