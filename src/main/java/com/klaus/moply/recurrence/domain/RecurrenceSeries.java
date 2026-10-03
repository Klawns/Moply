package com.klaus.moply.recurrence.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.klaus.moply.recurrence.domain.vo.GenerationWindow;
import com.klaus.moply.recurrence.domain.vo.RecurrencePeriod;
import com.klaus.moply.shared.domain.exception.DomainException;

import lombok.Getter;

@Getter
public final class RecurrenceSeries {

	private final UUID id;

	private final UUID organizationId;

	private final Frequency frequency;

	private final RecurrencePeriod period;

	private final WorkTemplate template;

	private RecurrenceSeries(UUID id, UUID organizationId, Frequency frequency, RecurrencePeriod period,
			WorkTemplate template) {
		if (id == null || organizationId == null || frequency == null || period == null || template == null) {
			throw new DomainException(
					"Série inválida: frequência, início e condições são obrigatórios; término não pode anteceder o início.");
		}
		this.id = id;
		this.organizationId = organizationId;
		this.frequency = frequency;
		this.period = period;
		this.template = template;
	}

	public static RecurrenceSeries create(UUID account, Frequency frequency, RecurrencePeriod period,
			WorkTemplate template) {
		return new RecurrenceSeries(UUID.randomUUID(), account, frequency, period, template);
	}

	public static RecurrenceSeries restore(UUID id, UUID account, Frequency frequency, RecurrencePeriod period,
			WorkTemplate template) {
		return new RecurrenceSeries(id, account, frequency, period, template);
	}

	/** Includes today and 29 subsequent dates. Never walks the missed history. */
	public List<LocalDate> occurrences(GenerationWindow window) {
		var from = effectiveStartDate(window);
		if (!from.isBefore(window.until()) || !period.contains(from)) {
			return List.of();
		}
		return frequency == Frequency.MONTHLY ? monthlyOccurrences(from, window) : weeklyOccurrences(from, window);
	}

	private LocalDate effectiveStartDate(GenerationWindow window) {
		return window.from().isAfter(period.startsOn()) ? window.from() : period.startsOn();
	}

	private List<LocalDate> monthlyOccurrences(LocalDate from, GenerationWindow window) {
		var result = new ArrayList<LocalDate>();
		var month = YearMonth.from(from);
		while (month.atDay(1).isBefore(window.until())) {
			var date = adjustedMonthlyDate(month);
			if (window.contains(date) && period.contains(date)) {
				result.add(date);
			}
			month = month.plusMonths(1);
		}
		return List.copyOf(result);
	}

	private LocalDate adjustedMonthlyDate(YearMonth month) {
		return month.atDay(Math.min(period.startsOn().getDayOfMonth(), month.lengthOfMonth()));
	}

	private List<LocalDate> weeklyOccurrences(LocalDate from, GenerationWindow window) {
		var result = new ArrayList<LocalDate>();
		int step = frequency == Frequency.WEEKLY ? 7 : 14;
		var date = firstAlignedOccurrence(from, step);
		while (window.contains(date) && period.contains(date)) {
			result.add(date);
			date = date.plusDays(step);
		}
		return List.copyOf(result);
	}

	private LocalDate firstAlignedOccurrence(LocalDate from, int step) {
		long elapsed = ChronoUnit.DAYS.between(period.startsOn(), from);
		return period.startsOn().plusDays(((elapsed + step - 1) / step) * step);

	}

}
