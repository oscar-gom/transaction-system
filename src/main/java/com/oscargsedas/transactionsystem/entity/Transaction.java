package com.oscargsedas.transactionsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "transaction")
public class Transaction extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false)
	private UUID id;

	@Column(name = "idempotency_key", nullable = false, unique = true)
	private UUID idempotencyKey;

	@ManyToOne(optional = false)
	@JoinColumn(name = "receiver_account_id", nullable = false)
	private Account receiverAccount;

	@ManyToOne(optional = false)
	@JoinColumn(name = "sender_account_id", nullable = false)
	private Account senderAccount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private TransactionStatus status;

}