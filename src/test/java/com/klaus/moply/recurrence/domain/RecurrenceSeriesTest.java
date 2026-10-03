package com.klaus.moply.recurrence.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.recurrence.domain.vo.GenerationWindow;
import com.klaus.moply.recurrence.domain.vo.RecurrenceParticipants;
import com.klaus.moply.recurrence.domain.vo.RecurrencePeriod;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;

import static org.junit.jupiter.api.Assertions.*;

class RecurrenceSeriesTest {

	WorkTemplate template() {
		return new WorkTemplate(UUID.randomUUID(), null, null, null, new DurationHours(BigDecimal.ONE),
				new HourlyRate(BigDecimal.TEN), "GBP", new RecurrenceParticipants(List.of(UUID.randomUUID())),
				WorkOrderStatus.SCHEDULED);
	}

	RecurrenceSeries series(Frequency f, String start, String end) {
		return RecurrenceSeries.create(UUID.randomUUID(), f,
				new RecurrencePeriod(LocalDate.parse(start), end == null ? null : LocalDate.parse(end)), template());
	}

	@Test
	void shouldIncludeExactlyTodayThroughDay29() {
		var today = LocalDate.of(2026, 10, 3);
		assertEquals(List.of(today, today.plusDays(7), today.plusDays(14), today.plusDays(21), today.plusDays(28)),
				series(Frequency.WEEKLY, "2026-10-03", null).occurrences(new GenerationWindow(today)));
		assertEquals(List.of(today, today.plusDays(14), today.plusDays(28)),
				series(Frequency.BIWEEKLY, "2026-10-03", null).occurrences(new GenerationWindow(today)));
		assertEquals(List.of(today.plusDays(2), today.plusDays(9), today.plusDays(16), today.plusDays(23)),
				series(Frequency.WEEKLY, "2026-10-05", null).occurrences(new GenerationWindow(today)));
	}

	@Test
	void shouldRespectStartAndInclusiveEnd() {
		var today = LocalDate.of(2026, 10, 3);
		assertTrue(series(Frequency.WEEKLY, "2026-11-02", null).occurrences(new GenerationWindow(today)).isEmpty());
		assertTrue(series(Frequency.WEEKLY, "2026-09-01", "2026-10-02").occurrences(new GenerationWindow(today))
			.isEmpty());
		assertEquals(List.of(today, today.plusDays(7)),
				series(Frequency.WEEKLY, "2026-10-03", "2026-10-10").occurrences(new GenerationWindow(today)));
		assertThrows(DomainException.class, () -> series(Frequency.WEEKLY, "2026-10-03", "2026-10-02"));
	}

	@Test
	void shouldRecoverOnlyCurrentWindowWithoutMovingWeeklyAnchor() {
		var dates = series(Frequency.BIWEEKLY, "2000-01-01", null)
			.occurrences(new GenerationWindow(LocalDate.of(2026, 10, 3)));
		assertTrue(dates.stream()
			.allMatch(d -> !d.isBefore(LocalDate.of(2026, 10, 3)) && d.isBefore(LocalDate.of(2026, 11, 2))));
		assertTrue(dates.stream()
			.allMatch(d -> java.time.temporal.ChronoUnit.DAYS.between(LocalDate.of(2000, 1, 1), d) % 14 == 0));
		assertFalse(dates.isEmpty());
	}

	@Test
	void shouldKeepOriginalMonthlyAnchorAfterShortMonthsAndLeapYears() {
		var s = series(Frequency.MONTHLY, "2024-01-31", null);
		assertEquals(List.of(LocalDate.of(2024, 2, 29)), s.occurrences(new GenerationWindow(LocalDate.of(2024, 2, 1))));
		assertEquals(List.of(LocalDate.of(2024, 3, 31)), s.occurrences(new GenerationWindow(LocalDate.of(2024, 3, 2))));
		assertEquals(List.of(LocalDate.of(2025, 2, 28)), s.occurrences(new GenerationWindow(LocalDate.of(2025, 2, 1))));
		assertEquals(List.of(LocalDate.of(2025, 4, 30)), s.occurrences(new GenerationWindow(LocalDate.of(2025, 4, 2))));
		assertEquals(List.of(LocalDate.of(2025, 5, 31)), s.occurrences(new GenerationWindow(LocalDate.of(2025, 5, 2))));
	}

	@Test
	void shouldSupportMonthlyDays29And30AndYearBoundary() {
		for (int day : List.of(29, 30)) {
			var s = series(Frequency.MONTHLY, "2025-01-" + day, null);
			assertEquals(List.of(LocalDate.of(2025, 2, 28)),
					s.occurrences(new GenerationWindow(LocalDate.of(2025, 2, 1))));
			assertEquals(List.of(LocalDate.of(2025, 3, day)),
					s.occurrences(new GenerationWindow(LocalDate.of(2025, 3, 1))));
		}
		assertEquals(List.of(LocalDate.of(2027, 1, 31)), series(Frequency.MONTHLY, "2026-12-31", null)
			.occurrences(new GenerationWindow(LocalDate.of(2027, 1, 2))));
	}

	@Test
	void shouldRestoreIdentityAndPreserveParticipantOrder() {
		var s = series(Frequency.WEEKLY, "2026-10-03", null);
		var restored = RecurrenceSeries.restore(s.getId(), s.getOrganizationId(), s.getFrequency(), s.getPeriod(),
				s.getTemplate());
		assertEquals(s.getId(), restored.getId());
		assertEquals(s.getTemplate().participants().ids(), restored.getTemplate().participants().ids());
		assertThrows(UnsupportedOperationException.class, () -> restored.getTemplate().participants().ids().clear());
		assertThrows(DomainException.class, () -> RecurrenceSeries.restore(null, s.getOrganizationId(),
				s.getFrequency(), s.getPeriod(), s.getTemplate()));
	}

}
