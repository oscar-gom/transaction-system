package com.oscargsedas.transactionsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
    private String email;
    private String name;
    private String surname;
    private String password;
}

