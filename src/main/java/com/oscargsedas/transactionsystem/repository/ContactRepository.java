package com.oscargsedas.transactionsystem.repository;

import com.oscargsedas.transactionsystem.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContactRepository extends JpaRepository<Contact, UUID> {
	Page<Contact> findAllByOwnerId(UUID ownerId, Pageable pageable);

	Page<Contact> findAllByOwnerIdAndFavoriteTrue(UUID ownerId, Pageable pageable);

	Optional<Contact> findByIdAndOwnerId(UUID id, UUID ownerId);

	boolean existsByOwnerIdAndContactAccountId(UUID ownerId, UUID contactAccountId);

	Page<Contact> findAllByOwnerIdAndAliasContainingIgnoreCase(UUID ownerId, String alias, Pageable pageable);
}
