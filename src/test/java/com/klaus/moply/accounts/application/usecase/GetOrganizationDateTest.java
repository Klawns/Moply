package com.klaus.moply.accounts.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.klaus.moply.accounts.application.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetOrganizationDateTest {

	private final UUID organizationId = UUID.randomUUID();

	private final Context context = new Context(organizationId);

	private final OrganizationRepository organizations = mock(OrganizationRepository.class);

	@ParameterizedTest
	@CsvSource({ "2026-09-30T23:30:00Z, Europe/London, 2026-10-01",
			"2026-09-30T23:30:00Z, America/Los_Angeles, 2026-09-30", "2026-10-25T00:30:00Z, Europe/London, 2026-10-25",
			"2026-10-25T01:30:00Z, Europe/London, 2026-10-25", "2026-03-29T00:30:00Z, Europe/London, 2026-03-29",
			"2026-03-29T01:30:00Z, Europe/London, 2026-03-29" })
	void shouldUseOrganizationTimezoneRegardlessOfClockZone(String instant, String zone, LocalDate expected) {
		when(organizations.findById(organizationId))
			.thenReturn(Optional.of(new Organization(organizationId, "Account", zone, DefaultWorkStatus.SCHEDULED)));
		var usecase = new GetOrganizationDate(organizations,
				Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Tokyo")));
		assertEquals(expected, usecase.execute(context, null));
		verify(organizations).findById(organizationId);
	}

	@Test
	void shouldRejectMissingOrganization() {
		var usecase = new GetOrganizationDate(organizations, Clock.systemUTC());
		assertThrows(AccountNotFoundException.class, () -> usecase.execute(context, null));
		verify(organizations).findById(organizationId);
	}

}
