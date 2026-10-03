package com.klaus.moply.recurrence.domain.vo;

import java.time.LocalDate;
import java.util.Objects;
import com.klaus.moply.shared.domain.exception.DomainException;

/** Inclusive period during which a series may occur. */
public record RecurrencePeriod(LocalDate startsOn, LocalDate endsOn) {
	public RecurrencePeriod {
		if (startsOn == null || (endsOn != null && endsOn.isBefore(startsOn)))
			throw new DomainException(
					"Série inválida: frequência, início e condições são obrigatórios; término não pode anteceder o início.");
	}

	public boolean contains(LocalDate date) {
		Objects.requireNonNull(date);
		return !date.isBefore(startsOn) && (endsOn == null || !date.isAfter(endsOn));
	}
}
