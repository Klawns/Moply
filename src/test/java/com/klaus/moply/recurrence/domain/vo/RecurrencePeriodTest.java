package com.klaus.moply.recurrence.domain.vo;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.klaus.moply.shared.domain.exception.DomainException;

class RecurrencePeriodTest {

	private final LocalDate date = LocalDate.of(2026, 12, 20);

	@Test
	void shouldRequireStartAndRejectInvertedDates() {
		assertThrows(DomainException.class, () -> new RecurrencePeriod(null, null));
		assertThrows(DomainException.class, () -> new RecurrencePeriod(null, date));
		assertThrows(DomainException.class, () -> new RecurrencePeriod(date, date.minusDays(1)));
	}

	@Test
	void shouldIncludeBothBoundsAndAllowSingleDay() {
		var period = new RecurrencePeriod(date, date.plusDays(7));
		assertTrue(period.contains(date));
		assertTrue(period.contains(date.plusDays(7)));
		assertFalse(period.contains(date.minusDays(1)));
		assertFalse(period.contains(date.plusDays(8)));
		assertTrue(new RecurrencePeriod(date, date).contains(date));
	}

	@Test
	void shouldAllowUnboundedEnd() {
		var period = new RecurrencePeriod(date, null);
		assertTrue(period.contains(date.plusYears(100)));
		assertFalse(period.contains(date.minusDays(1)));
	}

}
