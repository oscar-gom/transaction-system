package com.oscargsedas.transactionsystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ledger_lines", indexes = {
		@Index(name = "idx_transaction_id", columnList = "transaction_id"),
		@Index(name = "idx_account_id", columnList = "account_id")
})
@RequiredArgsConstructor
public class LedgerLine extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false)
	private UUID id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "transaction_id", nullable = false)
	private Transaction transaction;

	@ManyToOne(optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	private BigDecimal amount;
}