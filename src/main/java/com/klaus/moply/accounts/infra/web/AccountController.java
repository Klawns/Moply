package com.klaus.moply.accounts.infra.web;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.accounts.application.usecase.RegisterAccount;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/accounts")
public class AccountController {

	private final RegisterAccount register;

	@PostMapping()
	public ResponseEntity<RegistrationResponse> register(@RequestBody RegistrationRequest request) {
		var result = register.execute(
				new RegisterAccount.Input(request.name(), request.timezone(), request.email(), request.password()));
		return ResponseEntity.created(URI.create("/api/v1/accounts/me"))
			.body(new RegistrationResponse(result.organizationId(), result.userId()));
	}

	public record RegistrationRequest(String name, String timezone, String email, String password) {
		@Override
		public String toString() {
			return "RegistrationRequest[redacted]";
		}
	}

	public record RegistrationResponse(UUID organizationId, UUID userId) {
	}

}
