package com.klaus.moply.recurrence.domain.vo;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.klaus.moply.shared.domain.exception.DomainException;

class GenerationWindowTest {

	private final LocalDate date = LocalDate.of(2026, 12, 20);

	@Test
	void shouldRequireToday() {
		assertThrows(DomainException.class, () -> new GenerationWindow(null));
	}

	@Test
	void shouldIncludeTodayAndExcludeDay30AcrossYearBoundary() {
		var window = new GenerationWindow(date);
		assertEquals(date.plusDays(30), window.until());
		assertFalse(window.contains(date.minusDays(1)));
		assertTrue(window.contains(date));
		assertTrue(window.contains(date.plusDays(29)));
		assertFalse(window.contains(date.plusDays(30)));
	}

}
