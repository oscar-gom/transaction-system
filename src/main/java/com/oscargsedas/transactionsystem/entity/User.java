package com.oscargsedas.transactionsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users", indexes = {
		@Index(name = "idx_users_email", columnList = "email")
})
public class User extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(unique = true, nullable = false)
	@Email
	private String email;
	@NotNull
	private String name;
	@NotNull
	private String surname;
	@NotNull
	private String password;

	@OneToOne(mappedBy = "user")
	private Account account;

}
