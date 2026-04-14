package com.oscargsedas.transactionsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Table(name = "accounts", indexes = {
		@Index(name = "idx_accounts_user_id", columnList = "user_id"),
		@Index(name = "idx_accounts_currency", columnList = "currency")
})
public class Account extends BaseEntity {
	@Id
	@GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
	private UUID id;

	@ToString.Exclude
	@ManyToOne(optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 3)
	private String currency;
}
