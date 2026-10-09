package com.klaus.moply.accounts.application.usecase;

import com.klaus.moply.accounts.application.usecase.dto.UpdateAccountPreferencesInput;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateAccountPreferences implements Usecase.Contextual<UpdateAccountPreferencesInput, Void> {

	private final OrganizationRepository organizations;

	@Override
	public Void execute(Context context, UpdateAccountPreferencesInput input) {
		organizations
			.updatePreferences(context.organizationId(), organization -> organization.withPreferences(input.timezone(),
					input.defaultWorkStatus(),
					input.defaultHourlyRateProvided() ? input.defaultHourlyRate() : organization.defaultHourlyRate()));
		return null;
	}

}
