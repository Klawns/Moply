package com.klaus.moply.reports.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import com.klaus.moply.accounts.application.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.collaborators.application.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.reports.application.usecase.dto.ReportPeriod;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.domain.exception.DomainException;

public final class ReportContextResolver {

	private final OrganizationRepository organizations;

	private final Clock clock;

	private final CustomerRepository customers;

	private final CollaboratorRepository collaborators;

	public ReportContextResolver(OrganizationRepository organizations, Clock clock, CustomerRepository customers,
			CollaboratorRepository collaborators) {
		this.organizations = organizations;
		this.clock = clock;
		this.customers = customers;
		this.collaborators = collaborators;
	}

	public ReportContext resolve(Usecase.Context context, ReportPeriod period, UUID collaboratorId) {

		validatePeriod(period);
		validateCustomer(context, period);
		validateCollaborator(context, collaboratorId);

		var organization = findOrganization(context);

		return createReportContext(organization);
	}

	private void validatePeriod(ReportPeriod period) {
		if (period == null) {
			throw new DomainException("Informe o período.");
		}
	}

	private void validateCustomer(Usecase.Context context, ReportPeriod period) {

		if (period.customerId() == null) {
			return;
		}

		customers.findById(context.organizationId(), period.customerId())
			.orElseThrow(() -> new CustomerNotFoundException(period.customerId()));
	}

	private void validateCollaborator(Usecase.Context context, UUID collaboratorId) {

		if (collaboratorId == null) {
			return;
		}

		collaborators.findById(context.organizationId(), collaboratorId)
			.orElseThrow(() -> new CollaboratorNotFoundException(collaboratorId));
	}

	private Organization findOrganization(Usecase.Context context) {
		return organizations.findById(context.organizationId()).orElseThrow(AccountNotFoundException::new);
	}

	private ReportContext createReportContext(Organization organization) {
		var timezone = organization.timezone();
		var referenceDate = LocalDate.now(clock.withZone(ZoneId.of(timezone)));

		return new ReportContext(timezone, organization.currencyCode(), referenceDate);
	}

	public record ReportContext(String timezone, String currencyCode, LocalDate referenceDate) {
	}

}
