package com.klaus.moply.accounts.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import com.klaus.moply.accounts.application.usecase.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.shared.application.usecase.Usecase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetOrganizationDate implements Usecase.Contextual<Void, LocalDate> {

	private final OrganizationRepository organizations;

	private final Clock clock;

	@Override
	public LocalDate execute(Usecase.Context context, Void input) {
		var organization = organizations.findById(context.organizationId()).orElseThrow(AccountNotFoundException::new);
		return LocalDate.now(clock.withZone(ZoneId.of(organization.timezone())));
	}

}
