package com.klaus.moply.accounts.application.usecase;

import java.util.UUID;

import com.klaus.moply.accounts.application.ports.AccountRegistration;
import com.klaus.moply.accounts.application.ports.PasswordHasher;
import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.policy.PasswordPolicy;
import com.klaus.moply.accounts.domain.vo.LoginEmail;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.shared.application.usecase.Usecase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RegisterAccount implements Usecase<RegisterAccount.Input, RegisterAccount.Output> {

	private final AccountRegistration registration;

	private final PasswordHasher passwords;

	@Override
	public Output execute(Input input) {
		if (input == null) {
			throw new com.klaus.moply.shared.domain.exception.DomainException("Dados da conta obrigatórios.");
		}
		PasswordPolicy.validate(input.password());

		var organization = createOrganization(input);
		var manager = createManager(input, organization);

		registration.register(organization, manager);

		return new Output(organization.id(), manager.getId());
	}

	private Organization createOrganization(Input input) {
		return Organization.create(input.name(), input.timezone());
	}

	private AppUser createManager(Input input, Organization organization) {
		var email = new LoginEmail(input.email());
		var passwordHash = passwords.encode(input.password());

		return new AppUser(UUID.randomUUID(), organization.id(), email, passwordHash);
	}

	public record Input(String name, String timezone, String email, String password) {

		@Override
		public String toString() {
			return "RegisterAccount.Input[redacted]";
		}
	}

	public record Output(UUID organizationId, UUID userId) {
	}

}