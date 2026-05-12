package com.oscargsedas.transactionsystem.dto;

import com.oscargsedas.transactionsystem.entity.Account;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for {@link Account}
 */
public record AccountDto(Instant createdAt, Instant updatedAt, UUID id, UUID userId, String accountName,
                         @NotNull String currency) implements Serializable {
}