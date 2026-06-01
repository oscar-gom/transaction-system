package com.oscargsedas.transactionsystem.service;

import com.oscargsedas.transactionsystem.dto.ContactDto;
import com.oscargsedas.transactionsystem.dto.ContactRequest;
import com.oscargsedas.transactionsystem.dto.ContactUpdateRequest;
import com.oscargsedas.transactionsystem.dto.EntityDtoMapper;
import com.oscargsedas.transactionsystem.entity.Account;
import com.oscargsedas.transactionsystem.entity.Contact;
import com.oscargsedas.transactionsystem.entity.User;
import com.oscargsedas.transactionsystem.exception.ConflictException;
import com.oscargsedas.transactionsystem.exception.ForbiddenAccessException;
import com.oscargsedas.transactionsystem.exception.ResourceNotFoundException;
import com.oscargsedas.transactionsystem.repository.ContactRepository;
import com.oscargsedas.transactionsystem.util.AuthenticatedUserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactService {
	public static final int PAGE_SIZE = 10;
	private final ContactRepository contactRepository;
	private final AccountService accountService;
	private final EntityDtoMapper entityDtoMapper;
	private final AuthenticatedUserUtil authenticatedUserUtil;

	@Transactional
	public ContactDto createContact(ContactRequest request) {
		User authenticatedUser = authenticatedUserUtil.getAuthenticatedUser();
		Account targetAccount = accountService.getAnyAccountEntityById(request.contactAccountId());
		UUID ownerId = authenticatedUser.getId();

		if (targetAccount.getUser() != null && ownerId.equals(targetAccount.getUser().getId())) {
			throw new ForbiddenAccessException("You cannot add yourself as a contact");
		}

		if (contactRepository.existsByOwnerIdAndContactAccountId(ownerId, targetAccount.getId())) {
			throw new ConflictException("Contact already exists for this account");
		}

		Contact contact = new Contact();
		contact.setOwner(authenticatedUser);
		contact.setContactAccount(targetAccount);
		contact.setAlias(request.alias());
		contact.setNote(request.note());
		contact.setFavorite(request.favorite());

		return entityDtoMapper.toContactDto(contactRepository.save(contact));
	}

	public Page<ContactDto> getMyContacts(Pageable pageable, boolean favoritesOnly) {
		Pageable normalizedPageable = normalizePageable(pageable, Sort.by("alias"));
		UUID ownerId = authenticatedUserUtil.getAuthenticatedUserId();

		Page<Contact> contacts = favoritesOnly
				? contactRepository.findAllByOwnerIdAndFavoriteTrue(ownerId, normalizedPageable)
				: contactRepository.findAllByOwnerId(ownerId, normalizedPageable);

		return contacts.map(entityDtoMapper::toContactDto);
	}

	public ContactDto getMyContactById(UUID id) {
		UUID ownerId = authenticatedUserUtil.getAuthenticatedUserId();
		 /*Return 404 (not 403) when a contact id belongs to another user.
		 This is intentional: leaking "exists but not yours" via 403 would
		 let an attacker enumerate other users' contact ids. Keeping the
		 404 response is a deliberate safety choice.*/
		Contact contact = contactRepository.findByIdAndOwnerId(id, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + id));

		return entityDtoMapper.toContactDto(contact);
	}

	public Page<ContactDto> searchMyContactsByAlias(String alias, Pageable pageable) {
		if (alias == null) {
			throw new IllegalArgumentException("Search query must be at least 2 characters long");
		}

		String normalized = alias.trim();
		if (normalized.length() < 2) {
			throw new IllegalArgumentException("Search query must be at least 2 characters long");
		}

		Pageable normalizedPageable = normalizePageable(pageable, Sort.by("alias"));
		UUID ownerId = authenticatedUserUtil.getAuthenticatedUserId();

		return contactRepository.findAllByOwnerIdAndAliasContainingIgnoreCase(ownerId, normalized, normalizedPageable)
				.map(entityDtoMapper::toContactDto);
	}

	public ContactDto updateContact(UUID id, ContactUpdateRequest request) {
		UUID ownerId = authenticatedUserUtil.getAuthenticatedUserId();

		Contact contact = contactRepository.findByIdAndOwnerId(id, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + id));

		if (request.alias() != null) {
			contact.setAlias(request.alias());
		}
		if (request.note() != null) {
			contact.setNote(request.note());
		}
		if (request.favorite() != null) {
			contact.setFavorite(request.favorite());
		}

		return entityDtoMapper.toContactDto(contactRepository.save(contact));
	}

	public void deleteContact(UUID id) {
		UUID ownerId = authenticatedUserUtil.getAuthenticatedUserId();
		Contact contact = contactRepository.findByIdAndOwnerId(id, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Contact not found with id: " + id));

		contactRepository.delete(contact);
	}

	private Pageable normalizePageable(Pageable pageable, Sort sort) {
		if (pageable == null) {
			return PageRequest.of(0, PAGE_SIZE, sort);
		}
		if (pageable.getPageNumber() < 0) {
			throw new IllegalArgumentException("Page index must be zero or positive");
		}

		int requestedSize = pageable.getPageSize() <= 0 ? PAGE_SIZE : pageable.getPageSize();
		int safeSize = Math.min(requestedSize, PAGE_SIZE);

		Sort resolvedSort = pageable.getSort().isSorted() ? pageable.getSort() : sort;
		return PageRequest.of(Math.max(0, pageable.getPageNumber()), safeSize, resolvedSort);
	}
}
