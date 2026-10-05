package com.klaus.moply.recurrence.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.recurrence.domain.vo.GenerationWindow;
import com.klaus.moply.recurrence.domain.vo.RecurrenceParticipants;
import com.klaus.moply.recurrence.domain.vo.RecurrencePeriod;
import com.klaus.moply.recurrence.domain.vo.SeriesVersion;
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

	@Test
	void shouldKeepFamilyPositionsThroughMonthlyReanchoringAndCloseBeforeFirstDate() {
		var original = series(Frequency.MONTHLY, "2024-01-31", "2025-12-31");
		assertEquals(1, original.positionOf(LocalDate.of(2024, 2, 29)));
		var successor = original.successor(1, LocalDate.of(2024, 2, 28), null);
		assertEquals(original.getId(), successor.getLineage().familyId());
		assertEquals(2, successor.positionOf(LocalDate.of(2024, 3, 28)));
		assertEquals(LocalDate.of(2024, 4, 28), successor.dateAt(3));
		assertTrue(original.closeAt(0).occurrences(new GenerationWindow(LocalDate.of(2024, 1, 1))).isEmpty());
		assertThrows(DomainException.class, () -> original.positionOf(LocalDate.of(2024, 2, 28)));
		assertThrows(DomainException.class, () -> original.successor(1, LocalDate.of(2026, 1, 1), null));
	}

	@Test
	void shouldIntersectSuccessorBiweeklyCalendarWithThirtyDayWindowAndEnd() {
		var original = series(Frequency.BIWEEKLY, "2026-10-01", "2026-11-30");
		var successor = original.successor(2, LocalDate.of(2026, 10, 2), null);
		assertEquals(List.of(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 30)),
				successor.occurrences(new GenerationWindow(LocalDate.of(2026, 10, 3))));
		assertEquals(4, successor.positionOf(LocalDate.of(2026, 10, 30)));
		assertEquals(List.of(LocalDate.of(2026, 10, 16)),
				successor.closeAt(4).occurrences(new GenerationWindow(LocalDate.of(2026, 10, 3))));
	}

	@Test
	void shouldResolveFamilyPositionsBeyondVersionAndPeriodEnd() {
		var original = series(Frequency.WEEKLY, "2026-10-01", "2026-10-31");
		var successor = original.successor(2, LocalDate.of(2026, 10, 2), null).closeAt(3);
		assertEquals(LocalDate.of(2026, 11, 6), successor.dateAt(7));
		assertEquals(7, successor.positionOf(LocalDate.of(2026, 11, 6)));
		assertEquals("Posição anterior à versão.",
				assertThrows(DomainException.class, () -> successor.dateAt(1)).getMessage());
		var dates = successor.occurrences(new GenerationWindow(LocalDate.of(2026, 10, 1)));
		assertEquals(List.of(LocalDate.of(2026, 10, 2)), dates);
		assertThrows(UnsupportedOperationException.class, dates::clear);
	}

	@Test
	void shouldValidateLineageOnRestore() {
		var original = series(Frequency.WEEKLY, "2026-10-01", null);
		var id = original.getId();
		var familyId = UUID.randomUUID();
		var invalidLineages = new SeriesVersion[] { null, new SeriesVersion(id, id, 0, null),
				new SeriesVersion(familyId, null, 0, null), new SeriesVersion(id, null, 1, null),
				new SeriesVersion(id, UUID.randomUUID(), 1, null) };
		for (var lineage : invalidLineages) {
			assertEquals("Linhagem inválida.",
					assertThrows(DomainException.class, () -> RecurrenceSeries.restore(id, original.getOrganizationId(),
							original.getFrequency(), original.getPeriod(), original.getTemplate(), lineage))
						.getMessage());
		}
		var lineage = new SeriesVersion(familyId, UUID.randomUUID(), 2, 4L);
		var restored = RecurrenceSeries.restore(id, original.getOrganizationId(), original.getFrequency(),
				original.getPeriod(), original.getTemplate(), lineage);
		assertEquals(lineage, restored.getLineage());
		assertEquals(original.getFrequency(), restored.getFrequency());
		assertEquals(original.getPeriod(), restored.getPeriod());
	}

}
