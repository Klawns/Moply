package com.klaus.moply.accounts.infra.web;

import java.util.UUID;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonSetter;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.accounts.application.usecase.GetAccountPreferences;
import com.klaus.moply.accounts.application.usecase.UpdateAccountPreferences;
import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/accounts/me")
public class AccountPreferencesController {

	private final GetAccountPreferences preferences;

	private final UpdateAccountPreferences update;

	@GetMapping({ "", "/preferences" })
	public PreferencesResponse preferences(@AuthenticationPrincipal AccountPrincipal principal) {
		var result = preferences.execute(new Context(principal.getOrganizationId()), null);
		return new PreferencesResponse(result.organizationId(), result.name(), result.currencyCode(), result.timezone(),
				result.defaultWorkStatus(), result.today(), result.defaultHourlyRate());
	}

	@PutMapping("/preferences")
	public ResponseEntity<Void> update(@AuthenticationPrincipal AccountPrincipal principal,
			@RequestBody PreferencesRequest request) {
		update.execute(new Context(principal.getOrganizationId()),
				new UpdateAccountPreferences.Input(request.timezone(), request.defaultWorkStatus(),
						request.defaultHourlyRate(), request.defaultHourlyRateProvided()));
		return ResponseEntity.noContent().build();
	}

	public static class PreferencesRequest {

		private String timezone;

		private DefaultWorkStatus defaultWorkStatus;

		private BigDecimal defaultHourlyRate;

		private boolean defaultHourlyRateProvided;

		public PreferencesRequest() {
		}

		public String timezone() {
			return timezone;
		}

		public DefaultWorkStatus defaultWorkStatus() {
			return defaultWorkStatus;
		}

		public BigDecimal defaultHourlyRate() {
			return defaultHourlyRate;
		}

		public boolean defaultHourlyRateProvided() {
			return defaultHourlyRateProvided;
		}

		public void setTimezone(String timezone) {
			this.timezone = timezone;
		}

		public void setDefaultWorkStatus(DefaultWorkStatus status) {
			this.defaultWorkStatus = status;
		}

		@JsonSetter("defaultHourlyRate")
		public void setDefaultHourlyRate(BigDecimal value) {
			this.defaultHourlyRate = value;
			this.defaultHourlyRateProvided = true;
		}

	}

	public record PreferencesResponse(UUID organizationId, String name, String currencyCode, String timezone,
			DefaultWorkStatus defaultWorkStatus, java.time.LocalDate today, BigDecimal defaultHourlyRate) {
	}

}
