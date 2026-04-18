package com.oscargsedas.transactionsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
	@NotNull
	@Email(message = "email must be a valid email address")
    private String email;
	@NotNull
    private String name;
	@NotNull
    private String surname;
	@NotNull
    private String password;
}

