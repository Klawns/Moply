package com.klaus.moply.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.accounts.application.usecase.exception.AccountConflictException;
import com.klaus.moply.accounts.application.usecase.exception.AccountNotFoundException;
import com.klaus.moply.collaborators.application.usecase.exception.CollaboratorNotFoundException;
import com.klaus.moply.collaborators.domain.exception.InactiveCollaboratorException;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.exception.CustomerLocationNotFoundException;
import com.klaus.moply.payments.application.usecase.exception.PaymentConflictException;
import com.klaus.moply.payments.application.usecase.exception.PaymentNotFoundException;
import com.klaus.moply.recurrence.application.usecase.exception.SeriesNotFoundException;
import com.klaus.moply.shared.application.usecase.exception.ApplicationException;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.usecase.dto.PricingPreviewOutput;
import com.klaus.moply.workorders.application.usecase.exception.PricingAcceptanceException;
import com.klaus.moply.workorders.application.usecase.exception.WorkOrderNotFoundException;
import com.klaus.moply.workorders.domain.exception.WorkOrderStateException;

class CoreExceptionTest {

	@Test
	void shouldClassifyDomainAndApplicationFailuresWithStableCodes() {
		CoreException domain = new InactiveCollaboratorException();
		CoreException application = new AccountNotFoundException();
		ApplicationException genericApplication = new ApplicationException("Entrada inválida.");

		assertThat(domain.category()).isEqualTo(ErrorCategory.DOMAIN_ERROR);
		assertThat(domain.code()).isEqualTo("INACTIVE_COLLABORATOR");
		assertThat(domain).isInstanceOf(DomainException.class);

		assertThat(application.category()).isEqualTo(ErrorCategory.APPLICATION_ERROR);
		assertThat(application.code()).isEqualTo("ACCOUNT_NOT_FOUND");
		assertThat(application).isInstanceOf(ApplicationException.class);

		assertThat(genericApplication.category()).isEqualTo(ErrorCategory.APPLICATION_ERROR);
		assertThat(genericApplication.code()).isEqualTo("APPLICATION_ERROR");

		assertError(new WorkOrderStateException("Estado inválido."), ErrorCategory.DOMAIN_ERROR,
				"WORK_ORDER_STATE_ERROR");
		assertError(new CustomerLocationNotFoundException(UUID.randomUUID()), ErrorCategory.DOMAIN_ERROR,
				"CUSTOMER_LOCATION_NOT_FOUND");
		assertError(new AccountConflictException(), ErrorCategory.APPLICATION_ERROR, "ACCOUNT_CONFLICT");
		assertError(new CollaboratorNotFoundException(UUID.randomUUID()), ErrorCategory.APPLICATION_ERROR,
				"COLLABORATOR_NOT_FOUND");
		assertError(new CustomerNotFoundException(UUID.randomUUID()), ErrorCategory.APPLICATION_ERROR,
				"CUSTOMER_NOT_FOUND");
		assertError(new PaymentConflictException("Conflito."), ErrorCategory.APPLICATION_ERROR, "PAYMENT_CONFLICT");
		assertError(new PaymentNotFoundException(UUID.randomUUID()), ErrorCategory.APPLICATION_ERROR,
				"PAYMENT_NOT_FOUND");
		assertError(new SeriesNotFoundException(), ErrorCategory.APPLICATION_ERROR, "SERIES_NOT_FOUND");
		assertError(new WorkOrderNotFoundException(UUID.randomUUID()), ErrorCategory.APPLICATION_ERROR,
				"WORK_ORDER_NOT_FOUND");
		assertError(new PricingAcceptanceException(preview(true)), ErrorCategory.APPLICATION_ERROR,
				"PRICING_ACCEPTANCE_REQUIRED");
		assertError(new PricingAcceptanceException(preview(false)), ErrorCategory.APPLICATION_ERROR,
				"PRICING_BASES_EXCEED_TOTAL");
	}

	private void assertError(CoreException exception, ErrorCategory category, String code) {
		assertThat(exception.category()).isEqualTo(category);
		assertThat(exception.code()).isEqualTo(code);
	}

	private PricingPreviewOutput preview(boolean canCreate) {
		return new PricingPreviewOutput("fingerprint", "GBP", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 1,
				!canCreate, canCreate, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, 0, List.of());
	}

}
