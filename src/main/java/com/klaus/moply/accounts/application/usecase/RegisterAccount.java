package com.klaus.moply.accounts.application.usecase;

import java.util.UUID;

import com.klaus.moply.accounts.application.usecase.dto.RegisterAccountInput;
import com.klaus.moply.accounts.application.usecase.dto.RegisterAccountOutput;
import com.klaus.moply.accounts.application.ports.AccountRegistration;
import com.klaus.moply.accounts.application.ports.PasswordHasher;
import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.policy.PasswordPolicy;
import com.klaus.moply.accounts.domain.vo.LoginEmail;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.shared.application.usecase.Usecase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RegisterAccount implements Usecase<RegisterAccountInput, RegisterAccountOutput> {

	private final AccountRegistration registration;

	private final PasswordHasher passwords;

	@Override
	public RegisterAccountOutput execute(RegisterAccountInput input) {
		if (input == null) {
			throw new com.klaus.moply.shared.application.usecase.exception.ApplicationException(
					"Dados da conta obrigatórios.");
		}
		PasswordPolicy.validate(input.password());

		var organization = createOrganization(input);
		var manager = createManager(input, organization);

		registration.register(organization, manager);

		return new RegisterAccountOutput(organization.id(), manager.getId());
	}

	private Organization createOrganization(RegisterAccountInput input) {
		return Organization.create(input.name(), input.timezone());
	}

	private AppUser createManager(RegisterAccountInput input, Organization organization) {
		var email = new LoginEmail(input.email());
		var passwordHash = passwords.encode(input.password());

		return new AppUser(UUID.randomUUID(), organization.id(), email, passwordHash);
	}

}
