package com.klaus.moply.recurrence.domain.vo;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.klaus.moply.recurrence.domain.Frequency;
import com.klaus.moply.shared.domain.exception.DomainException;

import static org.junit.jupiter.api.Assertions.*;

class RecurrenceCalendarTest {

	private RecurrenceCalendar calendar(Frequency frequency, String start, String end) {
		return new RecurrenceCalendar(frequency,
				new RecurrencePeriod(LocalDate.parse(start), end == null ? null : LocalDate.parse(end)));
	}

	@Test
	void shouldIncludeExactlyTodayThroughDay29() {
		var today = LocalDate.of(2026, 10, 3);
		assertEquals(List.of(today, today.plusDays(7), today.plusDays(14), today.plusDays(21), today.plusDays(28)),
				calendar(Frequency.WEEKLY, "2026-10-03", null).occurrences(new GenerationWindow(today)));
		assertEquals(List.of(today, today.plusDays(14), today.plusDays(28)),
				calendar(Frequency.BIWEEKLY, "2026-10-03", null).occurrences(new GenerationWindow(today)));
		assertEquals(List.of(today.plusDays(2), today.plusDays(9), today.plusDays(16), today.plusDays(23)),
				calendar(Frequency.WEEKLY, "2026-10-05", null).occurrences(new GenerationWindow(today)));
	}

	@Test
	void shouldRespectStartAndInclusiveEnd() {
		var today = LocalDate.of(2026, 10, 3);
		assertTrue(calendar(Frequency.WEEKLY, "2026-11-02", null).occurrences(new GenerationWindow(today)).isEmpty());
		assertTrue(calendar(Frequency.WEEKLY, "2026-09-01", "2026-10-02").occurrences(new GenerationWindow(today))
			.isEmpty());
		assertEquals(List.of(today, today.plusDays(7)),
				calendar(Frequency.WEEKLY, "2026-10-03", "2026-10-10").occurrences(new GenerationWindow(today)));
		assertThrows(DomainException.class, () -> calendar(Frequency.WEEKLY, "2026-10-03", "2026-10-02"));
	}

	@Test
	void shouldRecoverOnlyCurrentWindowWithoutMovingWeeklyAnchor() {
		var dates = calendar(Frequency.BIWEEKLY, "2000-01-01", null)
			.occurrences(new GenerationWindow(LocalDate.of(2026, 10, 3)));
		assertTrue(dates.stream()
			.allMatch(d -> !d.isBefore(LocalDate.of(2026, 10, 3)) && d.isBefore(LocalDate.of(2026, 11, 2))));
		assertTrue(dates.stream()
			.allMatch(d -> java.time.temporal.ChronoUnit.DAYS.between(LocalDate.of(2000, 1, 1), d) % 14 == 0));
		assertFalse(dates.isEmpty());
	}

	@Test
	void shouldKeepOriginalMonthlyAnchorAfterShortMonthsAndLeapYears() {
		var s = calendar(Frequency.MONTHLY, "2024-01-31", null);
		assertEquals(List.of(LocalDate.of(2024, 2, 29)), s.occurrences(new GenerationWindow(LocalDate.of(2024, 2, 1))));
		assertEquals(List.of(LocalDate.of(2024, 3, 31)), s.occurrences(new GenerationWindow(LocalDate.of(2024, 3, 2))));
		assertEquals(List.of(LocalDate.of(2025, 2, 28)), s.occurrences(new GenerationWindow(LocalDate.of(2025, 2, 1))));
		assertEquals(List.of(LocalDate.of(2025, 4, 30)), s.occurrences(new GenerationWindow(LocalDate.of(2025, 4, 2))));
		assertEquals(List.of(LocalDate.of(2025, 5, 31)), s.occurrences(new GenerationWindow(LocalDate.of(2025, 5, 2))));
	}

	@Test
	void shouldSupportMonthlyDays29And30AndYearBoundary() {
		for (int day : List.of(29, 30)) {
			var s = calendar(Frequency.MONTHLY, "2025-01-" + day, null);
			assertEquals(List.of(LocalDate.of(2025, 2, 28)),
					s.occurrences(new GenerationWindow(LocalDate.of(2025, 2, 1))));
			assertEquals(List.of(LocalDate.of(2025, 3, day)),
					s.occurrences(new GenerationWindow(LocalDate.of(2025, 3, 1))));
		}
		assertEquals(List.of(LocalDate.of(2027, 1, 31)), calendar(Frequency.MONTHLY, "2026-12-31", null)
			.occurrences(new GenerationWindow(LocalDate.of(2027, 1, 2))));
	}

	@Test
	void shouldResolveDatesAndIndicesBeyondPeriodEnd() {
		for (var frequency : Frequency.values()) {
			var calendar = calendar(frequency, "2024-01-31", "2024-01-31");
			for (int index : List.of(0, 1, 2, 12, 13, 120)) {
				assertEquals(index, calendar.indexOf(calendar.dateAt(index)));
			}
		}
		var calendar = calendar(Frequency.MONTHLY, "2024-01-31", "2024-01-31");
		assertEquals(LocalDate.of(2024, 2, 29), calendar.dateAt(1));
		assertEquals(LocalDate.of(2024, 3, 31), calendar.dateAt(2));
	}

	@Test
	void shouldRejectInvalidCalendarInputs() {
		var period = new RecurrencePeriod(LocalDate.of(2024, 1, 31), null);
		assertThrows(DomainException.class, () -> new RecurrenceCalendar(null, period));
		assertThrows(DomainException.class, () -> new RecurrenceCalendar(Frequency.WEEKLY, null));
		for (var frequency : Frequency.values()) {
			var calendar = new RecurrenceCalendar(frequency, period);
			assertThrows(DomainException.class, () -> calendar.dateAt(-1));
			assertThrows(DomainException.class, () -> calendar.indexOf(null));
			assertThrows(DomainException.class, () -> calendar.indexOf(period.startsOn().minusDays(1)));
			assertEquals("Data fora do calendário original.",
					assertThrows(DomainException.class, () -> calendar.indexOf(period.startsOn().plusDays(1)))
						.getMessage());
		}
	}

	@Test
	void shouldSkipPastMonthlyOccurrenceAndExcludeWindowUpperBound() {
		var calendar = calendar(Frequency.MONTHLY, "2025-01-01", null);
		assertTrue(calendar.occurrences(new GenerationWindow(LocalDate.of(2025, 1, 2))).isEmpty());
		assertEquals(List.of(LocalDate.of(2025, 2, 1)),
				calendar.occurrences(new GenerationWindow(LocalDate.of(2025, 1, 3))));
	}

	@Test
	void shouldReturnImmutableOccurrences() {
		var calendar = calendar(Frequency.WEEKLY, "2026-10-03", null);
		var dates = calendar.occurrences(new GenerationWindow(LocalDate.of(2026, 10, 3)));
		assertThrows(UnsupportedOperationException.class, dates::clear);
	}

}
