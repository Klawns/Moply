package com.klaus.moply.recurrence.domain.vo;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.klaus.moply.recurrence.domain.Frequency;
import com.klaus.moply.shared.domain.exception.DomainException;

/** Recurrence dates indexed from zero at the start of the period. */
public record RecurrenceCalendar(Frequency frequency, RecurrencePeriod period) {

	public RecurrenceCalendar {
		if (frequency == null || period == null) {
			throw new DomainException(
					"Série inválida: frequência, início e condições são obrigatórios; término não pode anteceder o início.");
		}
	}

	/** Resolves a calendar date independently of the period end. */
	public LocalDate dateAt(long index) {
		if (index < 0) {
			throw new DomainException("Posição inválida.");
		}
		return switch (frequency) {
			case MONTHLY -> period.startsOn().plusMonths(index);
			case WEEKLY -> period.startsOn().plusDays(Math.multiplyExact(index, 7));
			case BIWEEKLY -> period.startsOn().plusDays(Math.multiplyExact(index, 14));
		};
	}

	/** Resolves an exact occurrence independently of the period end. */
	public long indexOf(LocalDate date) {
		if (date == null || date.isBefore(period.startsOn())) {
			throw new DomainException("Posição inválida.");
		}
		long index = estimatedIndex(date);
		if (!dateAt(index).equals(date)) {
			throw new DomainException("Data fora do calendário original.");
		}
		return index;
	}

	public List<LocalDate> occurrences(GenerationWindow window) {
		var from = window.from().isAfter(period.startsOn()) ? window.from() : period.startsOn();
		if (!from.isBefore(window.until()) || !period.contains(from)) {
			return List.of();
		}

		long index = estimatedIndex(from);
		var date = dateAt(index);
		if (date.isBefore(from)) {
			date = dateAt(++index);
		}

		var dates = new ArrayList<LocalDate>();
		while (window.contains(date) && period.contains(date)) {
			dates.add(date);
			date = dateAt(++index);
		}
		return List.copyOf(dates);
	}

	private long estimatedIndex(LocalDate date) {
		return switch (frequency) {
			case MONTHLY -> ChronoUnit.MONTHS.between(YearMonth.from(period.startsOn()), YearMonth.from(date));
			case WEEKLY -> ChronoUnit.DAYS.between(period.startsOn(), date) / 7;
			case BIWEEKLY -> ChronoUnit.DAYS.between(period.startsOn(), date) / 14;
		};
	}

}
