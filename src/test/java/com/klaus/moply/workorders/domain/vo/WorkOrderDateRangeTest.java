package com.klaus.moply.workorders.domain.vo;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.klaus.moply.shared.domain.exception.DomainException;

class WorkOrderDateRangeTest {

	private final LocalDate date = LocalDate.of(2026, 12, 20);

	@Test
	void shouldAllowEitherOrBothBoundsToBeAbsent() {
		assertTrue(new WorkOrderDateRange(null, null).contains(date));
		assertTrue(new WorkOrderDateRange(date, null).contains(date.plusYears(1)));
		assertFalse(new WorkOrderDateRange(date, null).contains(date.minusDays(1)));
		assertTrue(new WorkOrderDateRange(null, date).contains(date.minusYears(1)));
		assertFalse(new WorkOrderDateRange(null, date).contains(date.plusDays(1)));
	}

	@Test
	void shouldIncludeBothBoundsAndAllowSingleDay() {
		var range = new WorkOrderDateRange(date, date.plusDays(7));
		assertTrue(range.contains(date));
		assertTrue(range.contains(date.plusDays(7)));
		assertFalse(range.contains(date.minusDays(1)));
		assertFalse(range.contains(date.plusDays(8)));
		assertTrue(new WorkOrderDateRange(date, date).contains(date));
	}

	@Test
	void shouldRejectInvertedDates() {
		assertThrows(DomainException.class, () -> new WorkOrderDateRange(date, date.minusDays(1)));
	}

}
