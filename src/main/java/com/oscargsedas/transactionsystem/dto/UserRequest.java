package com.oscargsedas.transactionsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
	@NotBlank(message = "name is mandatory")
    private String name;
	@NotBlank(message = "surname is mandatory")
    private String surname;
	@NotBlank(message = "password is mandatory")
    private String password;
}

