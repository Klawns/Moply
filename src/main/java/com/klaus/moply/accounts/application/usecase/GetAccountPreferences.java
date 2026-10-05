package com.klaus.moply.accounts.application.usecase;

import java.time.Clock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import com.klaus.moply.accounts.application.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetAccountPreferences implements Usecase.Contextual<Void, GetAccountPreferences.Output> {

	private final OrganizationRepository organizations;

	private final Clock clock;

	@Override
	public Output execute(Context context, Void input) {
		var organization = organizations.findById(context.organizationId()).orElseThrow(AccountNotFoundException::new);
		return new Output(organization.id(), organization.name(), organization.currencyCode(), organization.timezone(),
				organization.defaultWorkStatus(), LocalDate.now(clock.withZone(ZoneId.of(organization.timezone()))),
				organization.defaultHourlyRate());
	}

	public record Output(UUID organizationId, String name, String currencyCode, String timezone,
			DefaultWorkStatus defaultWorkStatus, LocalDate today, BigDecimal defaultHourlyRate) {
	}

}
