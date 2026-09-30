package com.klaus.moply.accounts.application;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.klaus.moply.accounts.application.ports.*;
import com.klaus.moply.accounts.application.usecase.RegisterAccount;
import com.klaus.moply.accounts.domain.entities.AppUser;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.shared.domain.exception.DomainException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegisterAccountTest {

	private final AccountRegistration registration = mock(AccountRegistration.class);

	private final PasswordHasher passwords = mock(PasswordHasher.class);

	private final RegisterAccount usecase = new RegisterAccount(registration, passwords);

	@Test
	void shouldPersistLinkedAccountAndManagerWithEncodedPassword() {
		when(passwords.encode("a-long-password")).thenReturn("encoded");
		var output = usecase
			.execute(new RegisterAccount.Input("Empresa", "Europe/London", " Owner@Example.com ", "a-long-password"));
		var organization = ArgumentCaptor.forClass(Organization.class);
		var manager = ArgumentCaptor.forClass(AppUser.class);
		verify(registration).register(organization.capture(), manager.capture());
		assertEquals(output.organizationId(), organization.getValue().id());
		assertEquals(output.organizationId(), manager.getValue().getOrganizationId());
		assertEquals(output.userId(), manager.getValue().getId());
		assertEquals("owner@example.com", manager.getValue().getEmail().value());
		assertEquals("encoded", manager.getValue().getPasswordHash());
	}

	@Test
	void shouldRejectInvalidPasswordsBeforeEncodingOrPersistence() {
		for (String password : new String[] { null, "", "short", " ".repeat(12), "a".repeat(73), "é".repeat(37) }) {
			var input = new RegisterAccount.Input("Empresa", "UTC", "a@b", password);
			assertThrows(DomainException.class, () -> usecase.execute(input));
		}
		verifyNoInteractions(passwords, registration);
	}

	@Test
	void shouldAcceptPasswordsAtExactly72Utf8Bytes() {
		for (String password : new String[] { "a".repeat(72), "é".repeat(36) }) {
			when(passwords.encode(password)).thenReturn("encoded");
			usecase.execute(new RegisterAccount.Input("Empresa", "UTC", "a@b", password));
		}
		verify(passwords).encode("a".repeat(72));
		verify(passwords).encode("é".repeat(36));
		verify(registration, times(2)).register(any(), any());
	}

	@Test
	void shouldRejectNullRegistrationInputBeforeUsingDependencies() {
		assertThrows(DomainException.class, () -> usecase.execute(null));
		verifyNoInteractions(passwords, registration);
	}

}
