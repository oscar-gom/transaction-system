package com.oscargsedas.transactionsystem.dto;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Public facing DTO for Account searches to prevent data leakage.
 */
public record PublicAccountDto(
        Instant createdAt,
        UUID id,
        String accountName,
        @NotNull String currency
) implements Serializable {
}
