package com.oscargsedas.transactionsystem.exception;

import java.util.UUID;

public class CompletedIdempotencyKeyException extends RuntimeException {
	public CompletedIdempotencyKeyException(UUID idempotencyKey) {
		super("Transaction already completed for idempotency key: " + idempotencyKey);
	}
}

