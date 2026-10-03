package com.klaus.moply.recurrence.domain.vo;

import java.time.LocalDate;
import java.util.Objects;

import com.klaus.moply.shared.domain.exception.DomainException;

/** Includes today and the next 29 dates, excluding the upper bound. */
public record GenerationWindow(LocalDate from) {
	public GenerationWindow {
		if (from == null)
			throw new DomainException("Data inicial da janela de geração obrigatória.");
	}

	public LocalDate until() {
		return from.plusDays(30);
	}

	public boolean contains(LocalDate date) {
		Objects.requireNonNull(date);
		return !date.isBefore(from) && date.isBefore(until());
	}
}
