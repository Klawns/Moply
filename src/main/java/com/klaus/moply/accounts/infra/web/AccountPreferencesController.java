package com.klaus.moply.accounts.infra.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.accounts.application.usecase.GetAccountPreferences;
import com.klaus.moply.accounts.application.usecase.UpdateAccountPreferences;
import com.klaus.moply.accounts.infra.web.api.AccountPreferencesApi;
import com.klaus.moply.accounts.infra.web.dto.PreferencesRequest;
import com.klaus.moply.accounts.infra.web.dto.PreferencesResponse;
import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/accounts/me")
public class AccountPreferencesController implements AccountPreferencesApi {

	private final GetAccountPreferences preferences;

	private final UpdateAccountPreferences update;

	@GetMapping({ "", "/preferences" })
	@Override
	public PreferencesResponse preferences(@AuthenticationPrincipal AccountPrincipal principal) {
		var result = preferences.execute(new Context(principal.getOrganizationId()), null);
		return new PreferencesResponse(result.organizationId(), result.name(), result.currencyCode(), result.timezone(),
				result.defaultWorkStatus(), result.today(), result.defaultHourlyRate());
	}

	@PutMapping("/preferences")
	@Override
	public ResponseEntity<Void> update(@AuthenticationPrincipal AccountPrincipal principal,
			@RequestBody PreferencesRequest request) {
		update.execute(new Context(principal.getOrganizationId()),
				new UpdateAccountPreferences.Input(request.timezone(), request.defaultWorkStatus(),
						request.defaultHourlyRate(), request.defaultHourlyRateProvided()));
		return ResponseEntity.noContent().build();
	}

}
