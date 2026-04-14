package com.oscargsedas.transactionsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;
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
	private String name;
	private String surname;
	private String password;

	@OneToMany(mappedBy = "user")
	private Set<Account> accounts = new LinkedHashSet<>();

}
