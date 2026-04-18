package com.oscargsedas.transactionsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "transaction", indexes = {
		@Index(name = "idx_tx_sender", columnList = "sender_account_id"),
		@Index(name = "idx_tx_receiver", columnList = "receiver_account_id"),
		@Index(name = "idx_tx_idempotency_key", columnList = "idempotency_key"),
		@Index(name = "idx_tx_created", columnList = "created_at")
})
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

	@Digits(integer = 20, fraction = 2, message = "amount must be a valid number with up to 20 digits and 2 decimal places")
	@Column(name = "amount", nullable = false, precision = 22, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private TransactionStatus status = TransactionStatus.PENDING;

}