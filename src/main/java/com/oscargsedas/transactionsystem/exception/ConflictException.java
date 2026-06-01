package com.oscargsedas.transactionsystem.exception;

public class ConflictException extends RuntimeException {
	public ConflictException(String message) {
		super(message);
	}
}
