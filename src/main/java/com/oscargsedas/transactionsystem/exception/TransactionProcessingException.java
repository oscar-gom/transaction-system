package com.oscargsedas.transactionsystem.exception;

public class TransactionProcessingException extends RuntimeException {
	public TransactionProcessingException(String message, Throwable cause) {
		super(message, cause);
	}
}

