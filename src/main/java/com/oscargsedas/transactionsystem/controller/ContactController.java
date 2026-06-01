package com.oscargsedas.transactionsystem.controller;

import com.oscargsedas.transactionsystem.dto.ApiSuccessResponse;
import com.oscargsedas.transactionsystem.dto.ContactDto;
import com.oscargsedas.transactionsystem.dto.ContactRequest;
import com.oscargsedas.transactionsystem.dto.ContactUpdateRequest;
import com.oscargsedas.transactionsystem.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v4/contacts")
@RequiredArgsConstructor
@Tag(name = "Contacts", description = "Endpoints to manage user contacts")
public class ContactController {
	private final ContactService contactService;

	@GetMapping("/all")
	@Operation(summary = "List contacts", description = "List paginated contacts for the authenticated user")
	public ResponseEntity<ApiSuccessResponse<Page<ContactDto>>> getAllContacts(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "false") boolean favorites,
			HttpServletRequest request) {
		Pageable pageable = PageRequest.of(page, ContactService.PAGE_SIZE);
		Page<ContactDto> contacts = contactService.getMyContacts(pageable, favorites);

		ApiSuccessResponse<Page<ContactDto>> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Contacts retrieved successfully",
				request.getRequestURI(),
				contacts
		);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get contact", description = "Retrieve a contact by id. Only the owner can access it.")
	public ResponseEntity<ApiSuccessResponse<ContactDto>> getContactById(
			@PathVariable UUID id,
			HttpServletRequest request) {
		ContactDto contact = contactService.getMyContactById(id);

		ApiSuccessResponse<ContactDto> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Contact retrieved successfully",
				request.getRequestURI(),
				contact
		);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/search")
	@Operation(summary = "Search contacts", description = "Search contacts by alias containing the specified string. Returns paginated results.")
	public ResponseEntity<ApiSuccessResponse<Page<ContactDto>>> searchContacts(
			HttpServletRequest request,
			@RequestParam(name = "q") String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		int normalizedPage = Math.max(page, 0);
		Pageable pageable = PageRequest.of(normalizedPage, Math.clamp(size, 1, ContactService.PAGE_SIZE));
		Page<ContactDto> contacts = contactService.searchMyContactsByAlias(q, pageable);

		ApiSuccessResponse<Page<ContactDto>> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Contacts search results",
				request.getRequestURI(),
				contacts
		);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/create")
	@Operation(summary = "Create contact", description = "Create a contact for the authenticated user")
	public ResponseEntity<ApiSuccessResponse<ContactDto>> createContact(
			@Valid @RequestBody ContactRequest contactRequest,
			HttpServletRequest request) {
		ContactDto contact = contactService.createContact(contactRequest);

		ApiSuccessResponse<ContactDto> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.CREATED.value(),
				"Contact created successfully",
				request.getRequestURI(),
				contact
		);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/update/{id}")
	@Operation(summary = "Update contact", description = "Update alias, note, or favorite for the authenticated user's contact")
	public ResponseEntity<ApiSuccessResponse<ContactDto>> updateContact(
			@PathVariable UUID id,
			@Valid @RequestBody ContactUpdateRequest contactUpdateRequest,
			HttpServletRequest request) {
		ContactDto contact = contactService.updateContact(id, contactUpdateRequest);

		ApiSuccessResponse<ContactDto> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Contact updated successfully",
				request.getRequestURI(),
				contact
		);

		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/delete/{id}")
	@Operation(summary = "Delete contact", description = "Delete a contact for the authenticated user")
	public ResponseEntity<ApiSuccessResponse<Void>> deleteContact(
			@PathVariable UUID id,
			HttpServletRequest request) {
		contactService.deleteContact(id);

		ApiSuccessResponse<Void> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"Contact deleted successfully",
				request.getRequestURI(),
				null
		);

		return ResponseEntity.ok(response);
	}
}
