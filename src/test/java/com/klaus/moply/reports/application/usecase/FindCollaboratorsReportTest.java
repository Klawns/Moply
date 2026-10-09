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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.klaus.moply.reports.application.usecase.dto.ReportContext;
import com.klaus.moply.reports.application.ports.ReportReadRepository;
import com.klaus.moply.reports.application.usecase.dto.CollaboratorsReport;
import com.klaus.moply.reports.application.usecase.dto.CollaboratorsReportInput;
import com.klaus.moply.reports.application.usecase.dto.ReportPeriod;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.domain.exception.DomainException;

class FindCollaboratorsReportTest {

	private final ReportReadRepository reads = mock(ReportReadRepository.class);

	private final ReportContextResolver resolver = mock(ReportContextResolver.class);

	private final FindCollaboratorsReport usecase = new FindCollaboratorsReport(reads, resolver);

	private final Context context = new Context(UUID.randomUUID());

	private final LocalDate date = LocalDate.of(2026, 10, 1);

	@ParameterizedTest
	@CsvSource({ "COMPLETED,-1,true", "COMPLETED,0,true", "COMPLETED,1,false", "SCHEDULED,-1,false" })
	void shouldMapBalancesAndRealizationWhilePreservingFiltersAndIndependentTotals(String status, int days,
			boolean realized) {
		var customerId = UUID.randomUUID();
		var collaboratorId = UUID.randomUUID();
		var period = new ReportPeriod(date.minusDays(1), date.plusDays(1), customerId, new PageQuery(1, 2, null));
		var input = new CollaboratorsReportInput(period, collaboratorId);
		var assignment = new ReportReadRepository.AssignmentRow(UUID.randomUUID(), customerId, "Customer",
				date.plusDays(days), status, "GBP", collaboratorId, "Collaborator", BigDecimal.TEN,
				new BigDecimal("3.00"));
		var settlement = new ReportReadRepository.SettlementRow(UUID.randomUUID(), assignment.workOrderId(), customerId,
				"Customer", assignment.serviceDate(), status, collaboratorId, "Collaborator", date, "GBP",
				new BigDecimal("3.00"));
		when(resolver.resolve(context, period, collaboratorId)).thenReturn(new ReportContext("UTC", "GBP", date));
		when(reads.assignments(context.organizationId(), period.from(), period.to(), customerId, collaboratorId,
				period.page()))
			.thenReturn(new PageResult<>(List.of(assignment), 1, 2, 3, 2));
		when(reads.settlements(context.organizationId(), period.from(), period.to(), customerId, collaboratorId,
				period.page()))
			.thenReturn(new PageResult<>(List.of(settlement), 1, 2, 4, 2));
		var totals = new ReportReadRepository.CollaboratorTotals(new BigDecimal("100"), new BigDecimal("60"),
				new BigDecimal("40"), new BigDecimal("80"), new BigDecimal("50"), new BigDecimal("30"),
				new BigDecimal("20"));
		when(reads.collaboratorTotals(context.organizationId(), period.from(), period.to(), customerId, collaboratorId,
				date))
			.thenReturn(totals);
		var result = usecase.execute(context, input);
		assertEquals(new PageResult<>(List.of(new CollaboratorsReport.Assignment(assignment.workOrderId(), customerId,
				"Customer", assignment.serviceDate(), status, collaboratorId, "Collaborator", BigDecimal.TEN,
				new BigDecimal("3.00"), new BigDecimal("7.00"), realized)), 1, 2, 3, 2), result.assignments());
		assertEquals(new PageResult<>(List.of(new CollaboratorsReport.Settlement(settlement.paymentId(),
				assignment.workOrderId(), customerId, "Customer", assignment.serviceDate(), collaboratorId,
				"Collaborator", date, new BigDecimal("3.00"))), 1, 2, 4, 2), result.settlements());
		assertEquals(totals.allocated(), result.allocatedTotal());
		assertEquals(totals.realizedAllocated(), result.realizedAllocatedTotal());
		assertEquals(totals.futureAllocated(), result.futureAllocatedTotal());
		assertEquals(totals.pending(), result.pendingTotal());
		assertEquals(totals.realizedPending(), result.realizedPendingTotal());
		assertEquals(totals.futurePending(), result.futurePendingTotal());
		assertEquals(totals.settlements(), result.settlementsOnPeriodTotal());
		assertEquals(date, result.referenceDate());
	}

	@Test
	void shouldRejectMissingInputBeforeQueryingRepositories() {
		assertEquals("Informe o período.",
				assertThrows(DomainException.class, () -> usecase.execute(context, null)).getMessage());
		verifyNoInteractions(reads, resolver);
	}

}
