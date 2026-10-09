package com.klaus.moply.reports.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.accounts.application.usecase.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.collaborators.application.usecase.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.reports.application.usecase.dto.ReportPeriod;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.domain.exception.DomainException;

class ReportContextResolverTest {

	private final OrganizationRepository organizations = mock(OrganizationRepository.class);

	private final CustomerRepository customers = mock(CustomerRepository.class);

	private final CollaboratorRepository collaborators = mock(CollaboratorRepository.class);

	private final Context context = new Context(UUID.randomUUID());

	private final LocalDate date = LocalDate.of(2026, 10, 1);

	private final ReportContextResolver resolver = new ReportContextResolver(organizations,
			Clock.fixed(Instant.parse("2026-10-01T06:30:00Z"), ZoneOffset.UTC), customers, collaborators);

	@Test
	void shouldResolveDateAndPreferencesInOrganizationTimezone() {
		var organization = new Organization(context.organizationId(), "Reports", "America/Los_Angeles",
				DefaultWorkStatus.SCHEDULED);
		when(organizations.findById(context.organizationId())).thenReturn(Optional.of(organization));
		var result = resolver.resolve(context, new ReportPeriod(date, date, null), null);
		assertEquals(date.minusDays(1), result.referenceDate());
		assertEquals(organization.timezone(), result.timezone());
		assertEquals(organization.currencyCode(), result.currencyCode());
		verifyNoInteractions(customers, collaborators);
	}

	@Test
	void shouldRejectMissingOrganization() {
		assertThrows(AccountNotFoundException.class,
				() -> resolver.resolve(context, new ReportPeriod(date, date, null), null));
	}

	@Test
	void shouldRejectCustomerNotFoundInAuthenticatedOrganization() {
		var customerId = UUID.randomUUID();
		assertThrows(CustomerNotFoundException.class,
				() -> resolver.resolve(context, new ReportPeriod(date, date, customerId), null));
		verify(customers).findById(context.organizationId(), customerId);
		verifyNoInteractions(organizations, collaborators);
	}

	@Test
	void shouldRejectCollaboratorNotFoundInAuthenticatedOrganization() {
		var collaboratorId = UUID.randomUUID();
		assertThrows(CollaboratorNotFoundException.class,
				() -> resolver.resolve(context, new ReportPeriod(date, date, null), collaboratorId));
		verify(collaborators).findById(context.organizationId(), collaboratorId);
		verifyNoInteractions(organizations, customers);
	}

	@Test
	void shouldRejectMissingPeriodBeforeQueryingRepositories() {
		assertEquals("Informe o período.",
				assertThrows(DomainException.class, () -> resolver.resolve(context, null, null)).getMessage());
		verifyNoInteractions(organizations, customers, collaborators);
	}

}
