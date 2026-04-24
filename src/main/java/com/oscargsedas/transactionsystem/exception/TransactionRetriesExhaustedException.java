package com.oscargsedas.transactionsystem.exception;

import java.util.UUID;

public class TransactionRetriesExhaustedException extends RuntimeException {
	public TransactionRetriesExhaustedException(UUID idempotencyKey, Throwable cause) {
		super(buildMessage(idempotencyKey), cause);
	}

	private static String buildMessage(UUID idempotencyKey) {
		if (idempotencyKey == null) {
			return "Transaction failed after 3 retry attempts";
		}
		return "Transaction failed after 3 retry attempts for idempotency key: " + idempotencyKey;
	}
}

