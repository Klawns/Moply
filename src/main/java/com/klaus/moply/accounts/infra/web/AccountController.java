package com.klaus.moply.accounts.infra.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.accounts.application.usecase.dto.RegisterAccountInput;
import com.klaus.moply.accounts.application.usecase.RegisterAccount;
import com.klaus.moply.accounts.infra.web.api.AccountApi;
import com.klaus.moply.accounts.infra.web.dto.RegistrationRequest;
import com.klaus.moply.accounts.infra.web.dto.RegistrationResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/accounts")
public class AccountController implements AccountApi {

	private final RegisterAccount register;

	@PostMapping()
	@Override
	public ResponseEntity<RegistrationResponse> register(@RequestBody RegistrationRequest request) {
		var result = register
			.execute(new RegisterAccountInput(request.name(), request.timezone(), request.email(), request.password()));
		return ResponseEntity.created(URI.create("/api/v1/accounts/me"))
			.body(new RegistrationResponse(result.organizationId(), result.userId()));
	}

}
