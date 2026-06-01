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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

	@Mock
	private ContactRepository contactRepository;

	@Mock
	private AccountService accountService;

	@Mock
	private EntityDtoMapper entityDtoMapper;

	@Mock
	private AuthenticatedUserUtil authenticatedUserUtil;

	@InjectMocks
	private ContactService contactService;

	@Test
	void createContactPersistsAndReturnsDto() {
		UUID ownerId = UUID.randomUUID();
		UUID accountId = UUID.randomUUID();
		User owner = new User();
		owner.setId(ownerId);
		owner.setEmail("owner@example.com");

		Account targetAccount = new Account();
		targetAccount.setId(accountId);
		User otherUser = new User();
		otherUser.setId(UUID.randomUUID());
		targetAccount.setUser(otherUser);

		ContactDto mappedDto = mock(ContactDto.class);

		when(authenticatedUserUtil.getAuthenticatedUser()).thenReturn(owner);
		when(accountService.getAnyAccountEntityById(accountId)).thenReturn(targetAccount);
		when(contactRepository.existsByOwnerIdAndContactAccountId(ownerId, accountId)).thenReturn(false);
		when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(entityDtoMapper.toContactDto(any(Contact.class))).thenReturn(mappedDto);

		ContactDto result = contactService.createContact(new ContactRequest(accountId, "Mom", "Primary", true));

		assertEquals(mappedDto, result);
		verify(contactRepository).save(any(Contact.class));
	}

	@Test
	void createContactRejectsSelfContact() {
		UUID ownerId = UUID.randomUUID();
		UUID accountId = UUID.randomUUID();
		User owner = new User();
		owner.setId(ownerId);
		owner.setEmail("owner@example.com");

		Account targetAccount = new Account();
		targetAccount.setId(accountId);
		targetAccount.setUser(owner);

		when(authenticatedUserUtil.getAuthenticatedUser()).thenReturn(owner);
		when(accountService.getAnyAccountEntityById(accountId)).thenReturn(targetAccount);

		assertThrows(ForbiddenAccessException.class,
				() -> contactService.createContact(new ContactRequest(accountId, "Me", null, false)));

		verify(contactRepository, never()).save(any(Contact.class));
		verify(contactRepository, never()).existsByOwnerIdAndContactAccountId(any(UUID.class), any(UUID.class));
	}

	@Test
	void createContactRejectsDuplicate() {
		UUID ownerId = UUID.randomUUID();
		UUID accountId = UUID.randomUUID();
		User owner = new User();
		owner.setId(ownerId);
		owner.setEmail("owner@example.com");

		Account targetAccount = new Account();
		targetAccount.setId(accountId);
		User otherUser = new User();
		otherUser.setId(UUID.randomUUID());
		targetAccount.setUser(otherUser);

		when(authenticatedUserUtil.getAuthenticatedUser()).thenReturn(owner);
		when(accountService.getAnyAccountEntityById(accountId)).thenReturn(targetAccount);
		when(contactRepository.existsByOwnerIdAndContactAccountId(ownerId, accountId)).thenReturn(true);

		assertThrows(ConflictException.class,
				() -> contactService.createContact(new ContactRequest(accountId, "Mom", null, false)));

		verify(contactRepository, never()).save(any(Contact.class));
	}

	@Test
	void createContactRejectsMissingTargetAccount() {
		UUID ownerId = UUID.randomUUID();
		UUID accountId = UUID.randomUUID();
		User owner = new User();
		owner.setId(ownerId);
		owner.setEmail("owner@example.com");

		when(authenticatedUserUtil.getAuthenticatedUser()).thenReturn(owner);
		when(accountService.getAnyAccountEntityById(accountId))
				.thenThrow(new ResourceNotFoundException("Account not found"));

		assertThrows(ResourceNotFoundException.class,
				() -> contactService.createContact(new ContactRequest(accountId, "Mom", null, false)));

		verify(contactRepository, never()).save(any(Contact.class));
	}

	@Test
	void getMyContactsReturnsPagedDtosForAuthenticatedUser() {
		UUID ownerId = UUID.randomUUID();
		Contact c1 = new Contact();
		c1.setId(UUID.randomUUID());
		Contact c2 = new Contact();
		c2.setId(UUID.randomUUID());
		Page<Contact> page = new PageImpl<>(List.of(c1, c2), PageRequest.of(0, 10), 2);

		ContactDto d1 = mock(ContactDto.class);
		ContactDto d2 = mock(ContactDto.class);

		when(authenticatedUserUtil.getAuthenticatedUserId()).thenReturn(ownerId);
		when(contactRepository.findAllByOwnerId(eq(ownerId), any(PageRequest.class))).thenReturn(page);
		when(entityDtoMapper.toContactDto(c1)).thenReturn(d1);
		when(entityDtoMapper.toContactDto(c2)).thenReturn(d2);

		Page<ContactDto> result = contactService.getMyContacts(PageRequest.of(0, 10), false);

		assertEquals(2, result.getTotalElements());
		assertEquals(d1, result.getContent().get(0));
		assertEquals(d2, result.getContent().get(1));
	}

	@Test
	void getMyContactsFavoritesOnlyUsesFavoriteRepositoryMethod() {
		UUID ownerId = UUID.randomUUID();
		Page<Contact> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

		when(authenticatedUserUtil.getAuthenticatedUserId()).thenReturn(ownerId);
		when(contactRepository.findAllByOwnerIdAndFavoriteTrue(eq(ownerId), any(PageRequest.class))).thenReturn(page);

		Page<ContactDto> result = contactService.getMyContacts(PageRequest.of(0, 10), true);

		assertEquals(0, result.getTotalElements());
		verify(contactRepository).findAllByOwnerIdAndFavoriteTrue(eq(ownerId), any(PageRequest.class));
		verify(contactRepository, never()).findAllByOwnerId(eq(ownerId), any(PageRequest.class));
	}

	@Test
	void getMyContactByIdThrowsWhenContactBelongsToAnotherUser() {
		UUID ownerId = UUID.randomUUID();
		UUID contactId = UUID.randomUUID();

		when(authenticatedUserUtil.getAuthenticatedUserId()).thenReturn(ownerId);
		when(contactRepository.findByIdAndOwnerId(contactId, ownerId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> contactService.getMyContactById(contactId));
	}

	@Test
	void updateContactPatchesOnlyProvidedFields() {
		UUID ownerId = UUID.randomUUID();
		UUID contactId = UUID.randomUUID();
		Contact contact = new Contact();
		contact.setId(contactId);
		contact.setAlias("Old");
		contact.setNote("Old note");
		contact.setFavorite(false);

		ContactDto mappedDto = mock(ContactDto.class);

		when(authenticatedUserUtil.getAuthenticatedUserId()).thenReturn(ownerId);
		when(contactRepository.findByIdAndOwnerId(contactId, ownerId)).thenReturn(Optional.of(contact));
		when(contactRepository.save(contact)).thenReturn(contact);
		when(entityDtoMapper.toContactDto(contact)).thenReturn(mappedDto);

		ContactUpdateRequest request = new ContactUpdateRequest("New", null, null);
		ContactDto result = contactService.updateContact(contactId, request);

		assertEquals(mappedDto, result);
		assertEquals("New", contact.getAlias());
		assertEquals("Old note", contact.getNote());
		assertEquals(false, contact.isFavorite());
	}

	@Test
	void updateContactThrowsWhenNotOwner() {
		UUID ownerId = UUID.randomUUID();
		UUID contactId = UUID.randomUUID();

		when(authenticatedUserUtil.getAuthenticatedUserId()).thenReturn(ownerId);
		when(contactRepository.findByIdAndOwnerId(contactId, ownerId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> contactService.updateContact(contactId, new ContactUpdateRequest("New", null, null)));

		verify(contactRepository, never()).save(any(Contact.class));
	}

	@Test
	void deleteContactDeletesEntityWhenOwner() {
		UUID ownerId = UUID.randomUUID();
		UUID contactId = UUID.randomUUID();
		Contact contact = new Contact();
		contact.setId(contactId);

		when(authenticatedUserUtil.getAuthenticatedUserId()).thenReturn(ownerId);
		when(contactRepository.findByIdAndOwnerId(contactId, ownerId)).thenReturn(Optional.of(contact));

		contactService.deleteContact(contactId);

		verify(contactRepository).delete(contact);
	}

	@Test
	void deleteContactThrowsWhenNotOwner() {
		UUID ownerId = UUID.randomUUID();
		UUID contactId = UUID.randomUUID();

		when(authenticatedUserUtil.getAuthenticatedUserId()).thenReturn(ownerId);
		when(contactRepository.findByIdAndOwnerId(contactId, ownerId)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> contactService.deleteContact(contactId));
		verify(contactRepository, never()).delete(any(Contact.class));
	}

	@Test
	void searchMyContactsByAliasReturnsPagedDtos() {
		UUID ownerId = UUID.randomUUID();
		Contact c1 = new Contact();
		c1.setId(UUID.randomUUID());
		Contact c2 = new Contact();
		c2.setId(UUID.randomUUID());

		ContactDto d1 = mock(ContactDto.class);
		ContactDto d2 = mock(ContactDto.class);

		Page<Contact> page = new PageImpl<>(List.of(c1, c2), PageRequest.of(0, 10), 2);

		when(authenticatedUserUtil.getAuthenticatedUserId()).thenReturn(ownerId);
		when(contactRepository.findAllByOwnerIdAndAliasContainingIgnoreCase(eq(ownerId), eq("mo"), any(PageRequest.class))).thenReturn(page);
		when(entityDtoMapper.toContactDto(c1)).thenReturn(d1);
		when(entityDtoMapper.toContactDto(c2)).thenReturn(d2);

		Page<ContactDto> result = contactService.searchMyContactsByAlias("mo", PageRequest.of(0, 10));

		assertEquals(2, result.getTotalElements());
		assertEquals(d1, result.getContent().get(0));
		assertEquals(d2, result.getContent().get(1));
	}

	@Test
	void searchMyContactsByAliasThrowsForShortQuery() {
		assertThrows(IllegalArgumentException.class,
				() -> contactService.searchMyContactsByAlias("m", PageRequest.of(0, 10)));
	}
}
