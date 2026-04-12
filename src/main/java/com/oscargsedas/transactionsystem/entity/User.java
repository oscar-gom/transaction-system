package com.oscargsedas.transactionsystem.entity;

import jakarta.persistence.*;
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
@Table(name = "users")
public class User extends BaseEntity{
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(unique = true, nullable = false)
	private String email;
	private String name;
	private String surname;
	private String password;

	@OneToMany(mappedBy = "user")
	private Set<Account> accounts = new LinkedHashSet<>();

}
