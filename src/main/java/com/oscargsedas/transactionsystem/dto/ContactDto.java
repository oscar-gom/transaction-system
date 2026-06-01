package com.oscargsedas.transactionsystem.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record ContactDto(
		Instant createdAt,
		Instant updatedAt,
		UUID id,
		UUID ownerUserId,
		UUID contactAccountId,
		String contactAccountName,
		String alias,
		String note,
		boolean favorite
) implements Serializable {
}
