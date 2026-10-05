package com.klaus.moply.reports.infra.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.reports.application.ports.ReportReadRepository;
import com.klaus.moply.reports.application.usecase.FindCollaboratorsReport;
import com.klaus.moply.reports.application.usecase.FindCustomerPaymentsReport;
import com.klaus.moply.reports.application.usecase.FindWorkOrdersReport;
import com.klaus.moply.reports.application.usecase.ReportContextResolver;
import com.klaus.moply.reports.infra.transaction.TransactionalFindCollaboratorsReport;
import com.klaus.moply.reports.infra.transaction.TransactionalFindCustomerPaymentsReport;
import com.klaus.moply.reports.infra.transaction.TransactionalFindWorkOrdersReport;

@Configuration
public class ReportsConfig {

	@Bean
	ReportContextResolver reportContextResolver(OrganizationRepository organizations, Clock clock,
			CustomerRepository customers, CollaboratorRepository collaborators) {
		return new ReportContextResolver(organizations, clock, customers, collaborators);
	}

	@Bean
	FindWorkOrdersReport findWorkOrdersReport(ReportReadRepository reports, ReportContextResolver contextResolver) {
		return new TransactionalFindWorkOrdersReport(reports, contextResolver);
	}

	@Bean
	FindCustomerPaymentsReport findCustomerPaymentsReport(ReportReadRepository reports,
			ReportContextResolver contextResolver) {
		return new TransactionalFindCustomerPaymentsReport(reports, contextResolver);
	}

	@Bean
	FindCollaboratorsReport findCollaboratorsReport(ReportReadRepository reports,
			ReportContextResolver contextResolver) {
		return new TransactionalFindCollaboratorsReport(reports, contextResolver);
	}

}
