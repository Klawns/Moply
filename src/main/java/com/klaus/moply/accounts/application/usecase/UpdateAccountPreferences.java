package com.klaus.moply.accounts.application.usecase;

import com.klaus.moply.accounts.application.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateAccountPreferences implements Usecase.Contextual<UpdateAccountPreferences.Input, Void> {

	private final OrganizationRepository organizations;

	@Override
	public Void execute(Context context, Input input) {
		var organization = organizations.findById(context.organizationId()).orElseThrow(AccountNotFoundException::new);
		organizations.update(organization.withPreferences(input.timezone(), input.defaultWorkStatus()));
		return null;
	}

	public record Input(String timezone, DefaultWorkStatus defaultWorkStatus) {
	}

}
