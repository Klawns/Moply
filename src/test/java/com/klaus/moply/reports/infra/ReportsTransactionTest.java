package com.klaus.moply.reports.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.reports.application.ports.ReportReadRepository;
import com.klaus.moply.reports.application.usecase.FindCollaboratorsReport;
import com.klaus.moply.reports.application.usecase.FindCustomerPaymentsReport;
import com.klaus.moply.reports.application.usecase.FindWorkOrdersReport;
import com.klaus.moply.reports.infra.config.ReportsConfig;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.domain.exception.DomainException;

class ReportsTransactionTest {

	@Configuration(proxyBeanMethods = false)
	@EnableTransactionManagement(proxyTargetClass = true)
	static class Transactions {

	}

	@Test
	void shouldRegisterOneTransactionalBeanPerActionAndStartRepeatableReadBeforeValidation() {
		var transactions = mock(PlatformTransactionManager.class);
		var status = new SimpleTransactionStatus();
		when(transactions.getTransaction(any())).thenReturn(status);
		try (var application = new AnnotationConfigApplicationContext()) {
			application.register(ReportsConfig.class, Transactions.class);
			application.registerBean(PlatformTransactionManager.class, () -> transactions);
			application.registerBean(OrganizationRepository.class, () -> mock(OrganizationRepository.class));
			application.registerBean(CustomerRepository.class, () -> mock(CustomerRepository.class));
			application.registerBean(CollaboratorRepository.class, () -> mock(CollaboratorRepository.class));
			application.registerBean(ReportReadRepository.class, () -> mock(ReportReadRepository.class));
			application.registerBean(Clock.class, Clock::systemUTC);
			application.refresh();
			var context = new Context(UUID.randomUUID());
			assertEquals(1, application.getBeansOfType(FindWorkOrdersReport.class).size());
			assertEquals(1, application.getBeansOfType(FindCustomerPaymentsReport.class).size());
			assertEquals(1, application.getBeansOfType(FindCollaboratorsReport.class).size());
			assertThrows(DomainException.class,
					() -> application.getBean(FindWorkOrdersReport.class).execute(context, null));
			assertThrows(DomainException.class,
					() -> application.getBean(FindCustomerPaymentsReport.class).execute(context, null));
			assertThrows(DomainException.class,
					() -> application.getBean(FindCollaboratorsReport.class).execute(context, null));
			verify(transactions, times(3)).getTransaction(argThat(definition -> definition.isReadOnly()
					&& definition.getIsolationLevel() == TransactionDefinition.ISOLATION_REPEATABLE_READ));
			verify(transactions, times(3)).rollback(status);
		}
	}

}
