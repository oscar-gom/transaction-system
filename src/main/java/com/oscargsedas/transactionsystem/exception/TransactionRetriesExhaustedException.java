package com.oscargsedas.transactionsystem.exception;

import java.util.UUID;

public class TransactionRetriesExhaustedException extends RuntimeException {
	public TransactionRetriesExhaustedException(UUID idempotencyKey, Throwable cause) {
		super("Transaction failed after 3 retry attempts for idempotency key: " + idempotencyKey, cause);
	}
}

