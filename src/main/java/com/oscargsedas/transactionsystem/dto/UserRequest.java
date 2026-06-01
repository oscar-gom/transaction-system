package com.oscargsedas.transactionsystem.dto;

import jakarta.validation.constraints.*;
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
	@Size(min = 8, max = 32, message = "password must be between 8 and 32 characters")
	@Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).*$", message = "password must contain at least one uppercase letter and one number")
	private String password;
}
