package com.klaus.moply.reports.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.klaus.moply.reports.application.usecase.dto.ReportContext;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.accounts.domain.entities.DefaultWorkStatus;
import com.klaus.moply.accounts.domain.vo.Organization;
import com.klaus.moply.collaborators.application.ports.CollaboratorRepository;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.reports.application.ports.ReportReadRepository;
import com.klaus.moply.reports.application.usecase.dto.ReportPeriod;
import com.klaus.moply.reports.application.usecase.dto.WorkOrdersReport;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

class FindWorkOrdersReportTest {

	@Test
	void shouldResolveReferenceDateInAccountTimezoneWithControlledClock() {
		var accountId = UUID.randomUUID();
		var account = mock(OrganizationRepository.class);
		var reads = mock(ReportReadRepository.class);
		var customers = mock(CustomerRepository.class);
		var collaborators = mock(CollaboratorRepository.class);
		when(account.findById(accountId)).thenReturn(Optional
			.of(new Organization(accountId, "Account", "America/Los_Angeles", DefaultWorkStatus.SCHEDULED)));
		when(reads.workRows(accountId, LocalDate.parse("2026-09-30"), LocalDate.parse("2026-10-01"), null,
				PageQuery.defaults()))
			.thenReturn(new PageResult<>(List.of(row("2026-09-30"), row("2026-10-01")), 0, 20, 2, 1));
		when(reads.workTotals(accountId, LocalDate.parse("2026-09-30"), LocalDate.parse("2026-10-01"), null,
				LocalDate.parse("2026-09-30")))
			.thenReturn(new ReportReadRepository.WorkTotals(new BigDecimal("20.00"), BigDecimal.ZERO,
					new BigDecimal("40.00")));
		var clock = Clock.fixed(Instant.parse("2026-10-01T06:30:00Z"), ZoneOffset.UTC);
		var report = new FindWorkOrdersReport(reads,
				new ReportContextResolver(account, clock, customers, collaborators))
			.execute(new Context(accountId),
					new ReportPeriod(LocalDate.parse("2026-09-30"), LocalDate.parse("2026-10-01"), null));
		assertEquals(LocalDate.parse("2026-09-30"), report.referenceDate());
		assertEquals(new BigDecimal("20.00"), report.realizedAmount());
		assertEquals(1, report.works().stream().filter(Work -> Work.realized()).count());
	}

	@ParameterizedTest
	@CsvSource({ "COMPLETED,-1,false,true,true", "COMPLETED,0,false,true,true", "COMPLETED,0,true,true,false",
			"COMPLETED,1,false,false,false", "SCHEDULED,-1,false,false,false" })
	void shouldClassifyRealizedAndPendingWorkAndPreservePagination(String status, int days, boolean paid,
			boolean realized, boolean pending) {
		var context = new Context(UUID.randomUUID());
		var date = LocalDate.of(2026, 10, 1);
		var period = new ReportPeriod(date.minusDays(1), date.plusDays(1), UUID.randomUUID(),
				new PageQuery(1, 2, null));
		var reads = mock(ReportReadRepository.class);
		var resolver = mock(ReportContextResolver.class);
		var row = new ReportReadRepository.WorkRow(UUID.randomUUID(), period.customerId(), "Customer",
				date.plusDays(days), status, "GBP", BigDecimal.TEN, paid);
		when(resolver.resolve(context, period, null)).thenReturn(new ReportContext("UTC", "GBP", date));
		when(reads.workRows(context.organizationId(), period.from(), period.to(), period.customerId(), period.page()))
			.thenReturn(new PageResult<>(List.of(row), 1, 2, 3, 2));
		when(reads.workTotals(context.organizationId(), period.from(), period.to(), period.customerId(), date))
			.thenReturn(new ReportReadRepository.WorkTotals(new BigDecimal("100"), new BigDecimal("60"),
					new BigDecimal("150")));
		var result = new FindWorkOrdersReport(reads, resolver).execute(context, period);
		assertEquals(
				new PageResult<>(List.of(new WorkOrdersReport.Work(row.workOrderId(), row.customerId(), "Customer",
						row.serviceDate(), status, BigDecimal.TEN, realized, paid, pending)), 1, 2, 3, 2),
				result.works());
		assertEquals(new BigDecimal("100"), result.realizedAmount());
		assertEquals(new BigDecimal("60"), result.realizedPendingAmount());
		assertEquals(new BigDecimal("150"), result.workProjectionAmount());
	}

	private ReportReadRepository.WorkRow row(String date) {
		return new ReportReadRepository.WorkRow(UUID.randomUUID(), UUID.randomUUID(), "Customer", LocalDate.parse(date),
				"COMPLETED", "GBP", new BigDecimal("20.00"), false);
	}

}
