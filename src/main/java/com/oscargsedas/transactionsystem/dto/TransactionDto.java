package com.oscargsedas.transactionsystem.dto;

import com.oscargsedas.transactionsystem.entity.Transaction;
import com.oscargsedas.transactionsystem.entity.TransactionStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for {@link Transaction}
 */
public record TransactionDto(Instant createdAt, Instant updatedAt, UUID id, AccountDto receiverAccount,
                             AccountDto senderAccount,
                             UUID idempotencyKey,
                             BigDecimal amount,
                             TransactionStatus status) implements Serializable {
}