package com.klaus.moply.reports.application.usecase.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.domain.exception.DomainException;

class ReportPeriodTest {

	private final LocalDate date = LocalDate.of(2026, 10, 1);

	@Test
	void shouldDefaultPaginationAndAcceptInclusiveSingleDayPeriod() {
		assertEquals(PageQuery.defaults(), new ReportPeriod(date, date, null).page());
		assertEquals(PageQuery.defaults(), new ReportPeriod(date, date, null, null).page());
	}

	@Test
	void shouldRejectMissingOrReversedDates() {
		assertThrows(DomainException.class, () -> new ReportPeriod(null, date, null));
		assertThrows(DomainException.class, () -> new ReportPeriod(date, null, null));
		assertThrows(DomainException.class, () -> new ReportPeriod(date, date.minusDays(1), null));
	}

	@Test
	void shouldRejectMissingCollaboratorReportPeriod() {
		assertEquals("Informe o período.",
				assertThrows(DomainException.class, () -> new CollaboratorsReportInput(null, null)).getMessage());
	}

}
