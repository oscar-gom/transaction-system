package com.oscargsedas.transactionsystem.controller;

import com.oscargsedas.transactionsystem.dto.ApiSuccessResponse;
import com.oscargsedas.transactionsystem.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v4/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative operations")
public class AdminController {
	private final AdminService adminService;

	@PostMapping("/promote")
	@Operation(summary = "Promote user to admin", description = "Promotes the user with the given email to ADMIN")
	public ResponseEntity<ApiSuccessResponse<Void>> promoteToAdmin(
			@RequestParam String email,
			HttpServletRequest request) {
		adminService.promoteToAdmin(email);

		ApiSuccessResponse<Void> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"User promoted to admin successfully",
				request.getRequestURI(),
				null
		);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/demote")
	@Operation(summary = "Demote admin to user", description = "Demotes the user with the given email to USER")
	public ResponseEntity<ApiSuccessResponse<Void>> demoteFromAdmin(
			@RequestParam String email,
			HttpServletRequest request) {
		adminService.demoteFromAdmin(email);

		ApiSuccessResponse<Void> response = new ApiSuccessResponse<>(
				Instant.now(),
				HttpStatus.OK.value(),
				"User demoted from admin successfully",
				request.getRequestURI(),
				null
		);

		return ResponseEntity.ok(response);
	}
}
