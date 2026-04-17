package com.oscargsedas.transactionsystem.exception;

public class ForbiddenAccessException extends RuntimeException {
	public ForbiddenAccessException(String message) {
		super(message);
	}
}

