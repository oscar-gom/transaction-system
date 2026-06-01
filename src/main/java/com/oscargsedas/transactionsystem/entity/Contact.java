package com.oscargsedas.transactionsystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "contacts", indexes = {
		@Index(name = "idx_contacts_owner", columnList = "owner_user_id"),
		@Index(name = "idx_contacts_owner_favorite", columnList = "owner_user_id, favorite")
}, uniqueConstraints = {
		@UniqueConstraint(name = "uc_contacts_owner_target", columnNames = {"owner_user_id", "contact_account_id"})
})
public class Contact extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ToString.Exclude
	@ManyToOne(optional = false)
	@JoinColumn(name = "owner_user_id", nullable = false)
	private User owner;

	@ToString.Exclude
	@ManyToOne(optional = false)
	@JoinColumn(name = "contact_account_id", nullable = false)
	private Account contactAccount;

	@Column(nullable = false, length = 60)
	private String alias;

	@Column(length = 255)
	private String note;

	@Column(nullable = false)
	private boolean favorite = false;
}
