package com.klaus.moply.accounts.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import com.klaus.moply.accounts.application.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.application.usecase.GetAccountPreferences;
import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.accounts.domain.vo.Organization;

class AccountPreferencesTest {

	@Test
	void shouldComputeTodayInAccountTimezoneUsingInjectedClock() {
		var repo = mock(OrganizationRepository.class);
		var account = Organization.create("Empresa", "America/Los_Angeles");
		when(repo.findById(account.id())).thenReturn(Optional.of(account));
		var clock = Clock.fixed(Instant.parse("2026-09-28T00:30:00Z"), ZoneOffset.UTC);
		var usecase = new GetAccountPreferences(repo, clock);
		assertEquals(LocalDate.of(2026, 9, 27), usecase.execute(new Context(account.id()), null).today());
		when(repo.findById(account.id()))
			.thenReturn(Optional.of(account.withPreferences("Europe/London", DefaultWorkStatus.COMPLETED)));
		assertEquals(LocalDate.of(2026, 9, 28), usecase.execute(new Context(account.id()), null).today());
	}

	@Test
	void shouldFailForMissingAccountInsteadOfUsingGlobalDefaults() {
		var repo = mock(OrganizationRepository.class);
		assertThrows(AccountNotFoundException.class, () -> new GetAccountPreferences(repo, Clock.systemUTC())
			.execute(new Context(java.util.UUID.randomUUID()), null));
	}

}
