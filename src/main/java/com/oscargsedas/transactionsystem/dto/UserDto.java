package com.oscargsedas.transactionsystem.dto;

import com.oscargsedas.transactionsystem.entity.User;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for {@link User}
 */
public record UserDto(Instant createdAt, Instant updatedAt, UUID id, String email, String name,
                      String surname) implements Serializable {
}