package com.oscargsedas.transactionsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String message;
    private String token;
    private boolean success;
    private UUID userId;
}

