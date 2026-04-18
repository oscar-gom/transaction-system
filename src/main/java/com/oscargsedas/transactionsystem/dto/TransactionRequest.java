package com.oscargsedas.transactionsystem.dto;

import java.util.UUID;

public record TransactionRequest(
		UUID senderId,
		UUID receiverId,
		UUID idempotencyKey
) { }
