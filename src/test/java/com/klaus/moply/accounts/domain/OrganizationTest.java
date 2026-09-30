package com.klaus.moply.accounts.domain;

import org.junit.jupiter.api.Test;

import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.accounts.domain.vo.LoginEmail;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.shared.domain.exception.DomainException;
import static org.junit.jupiter.api.Assertions.*;

class OrganizationTest {

	@Test
	void shouldCreateGbpAccountWithScheduledDefault() {
		var account = Organization.create("  Moply  ", "Europe/London");
		assertNotNull(account.id());
		assertEquals("Moply", account.name());
		assertEquals("GBP", account.currencyCode());
		assertEquals(DefaultWorkStatus.SCHEDULED, account.defaultWorkStatus());
		var changed = account.withPreferences("America/Sao_Paulo", DefaultWorkStatus.COMPLETED);
		assertEquals(account.id(), changed.id());
		assertEquals("GBP", changed.currencyCode());
		assertEquals(DefaultWorkStatus.SCHEDULED, account.defaultWorkStatus());
	}

	@Test
	void shouldRejectInvalidAccountPreferences() {
		assertThrows(DomainException.class, () -> Organization.create(" ", "Europe/London"));
		assertThrows(DomainException.class, () -> Organization.create("Empresa", "Mars/Olympus"));
		assertThrows(DomainException.class, () -> Organization.create("Empresa", "+03:00"));
		assertThrows(DomainException.class, () -> Organization.create("Empresa", null));
		assertThrows(DomainException.class, () -> Organization.create("Empresa", "UTC").withPreferences("UTC", null));
	}

	@Test
	void shouldNormalizeLoginEmailAndRejectInvalidValues() {
		assertEquals(new LoginEmail("manager@example.com"), new LoginEmail(" Manager@EXAMPLE.com "));
		for (String value : new String[] { null, "", "a@@b", "@b", "a@", "a b@c" }) {
			assertThrows(DomainException.class, () -> new LoginEmail(value));
		}
	}

}
