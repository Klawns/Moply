package com.klaus.moply.accounts.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import com.klaus.moply.accounts.application.usecase.dto.GetAccountPreferencesOutput;
import com.klaus.moply.accounts.application.usecase.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetAccountPreferences implements Usecase.Contextual<Void, GetAccountPreferencesOutput> {

	private final OrganizationRepository organizations;

	private final Clock clock;

	@Override
	public GetAccountPreferencesOutput execute(Context context, Void input) {
		var organization = organizations.findById(context.organizationId()).orElseThrow(AccountNotFoundException::new);
		return new GetAccountPreferencesOutput(organization.id(), organization.name(), organization.currencyCode(),
				organization.timezone(), organization.defaultWorkStatus(),
				LocalDate.now(clock.withZone(ZoneId.of(organization.timezone()))), organization.defaultHourlyRate());
	}

}
