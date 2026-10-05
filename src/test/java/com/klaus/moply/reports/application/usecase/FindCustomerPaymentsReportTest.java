package com.klaus.moply.reports.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.reports.application.ports.ReportReadRepository;
import com.klaus.moply.reports.application.usecase.dto.CustomerPaymentsReport;
import com.klaus.moply.reports.application.usecase.dto.ReportPeriod;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.domain.exception.DomainException;

class FindCustomerPaymentsReportTest {

	private final ReportReadRepository reads = mock(ReportReadRepository.class);

	private final ReportContextResolver resolver = mock(ReportContextResolver.class);

	private final FindCustomerPaymentsReport usecase = new FindCustomerPaymentsReport(reads, resolver);

	private final Context context = new Context(UUID.randomUUID());

	private final LocalDate date = LocalDate.of(2026, 10, 1);

	@Test
	void shouldPreservePaymentDetailsPaginationAndFullPeriodTotal() {
		var customerId = UUID.randomUUID();
		var period = new ReportPeriod(date, date, customerId, new PageQuery(1, 2, null));
		var row = new ReportReadRepository.PaymentRow(UUID.randomUUID(), UUID.randomUUID(), customerId, "Customer",
				date, "GBP", BigDecimal.TEN);
		when(resolver.resolve(context, period, null))
			.thenReturn(new ReportContextResolver.ReportContext("UTC", "GBP", date));
		when(reads.customerPayments(context.organizationId(), date, date, customerId, period.page()))
			.thenReturn(new PageResult<>(List.of(row), 1, 2, 3, 2));
		when(reads.customerPaymentTotal(context.organizationId(), date, date, customerId))
			.thenReturn(new BigDecimal("75.00"));
		var result = usecase.execute(context, period);
		assertEquals(new BigDecimal("75.00"), result.totalAmount());
		assertEquals(new PageResult<>(List.of(new CustomerPaymentsReport.Payment(row.paymentId(), row.workOrderId(),
				customerId, "Customer", date, BigDecimal.TEN)), 1, 2, 3, 2), result.payments());
		assertEquals("UTC", result.timezone());
		assertEquals("GBP", result.currencyCode());
		assertEquals(date, result.from());
		assertEquals(date, result.to());
	}

	@Test
	void shouldStopBeforeReadingPaymentsWhenPeriodIsMissing() {
		when(resolver.resolve(context, null, null)).thenThrow(new DomainException("Informe o período."));
		assertThrows(DomainException.class, () -> usecase.execute(context, null));
		verifyNoInteractions(reads);
	}

}
