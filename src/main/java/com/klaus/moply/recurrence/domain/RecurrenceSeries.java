package com.klaus.moply.recurrence.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.klaus.moply.recurrence.domain.vo.GenerationWindow;
import com.klaus.moply.recurrence.domain.vo.RecurrenceCalendar;
import com.klaus.moply.recurrence.domain.vo.RecurrencePeriod;
import com.klaus.moply.recurrence.domain.vo.SeriesVersion;
import com.klaus.moply.shared.domain.exception.DomainException;

import lombok.AccessLevel;
import lombok.Getter;

@Getter
public final class RecurrenceSeries {

	private final UUID id;

	private final UUID organizationId;

	@Getter(AccessLevel.NONE)
	private final RecurrenceCalendar calendar;

	private final WorkTemplate template;

	private final SeriesVersion lineage;

	private RecurrenceSeries(UUID id, UUID organizationId, Frequency frequency, RecurrencePeriod period,
			WorkTemplate template) {
		this(id, organizationId, frequency, period, template, new SeriesVersion(id, null, 0, null));
	}

	private RecurrenceSeries(UUID id, UUID organizationId, Frequency frequency, RecurrencePeriod period,
			WorkTemplate template, SeriesVersion lineage) {
		validate(id, organizationId, frequency, period, template);
		this.id = id;
		this.organizationId = organizationId;
		this.calendar = new RecurrenceCalendar(frequency, period);
		this.template = template;
		validateLineage(id, lineage);
		this.lineage = lineage;
	}

	public static RecurrenceSeries create(UUID account, Frequency frequency, RecurrencePeriod period,
			WorkTemplate template) {
		return new RecurrenceSeries(UUID.randomUUID(), account, frequency, period, template);
	}

	public static RecurrenceSeries restore(UUID id, UUID account, Frequency frequency, RecurrencePeriod period,
			WorkTemplate template) {
		return new RecurrenceSeries(id, account, frequency, period, template);
	}

	public static RecurrenceSeries restore(UUID id, UUID account, Frequency frequency, RecurrencePeriod period,
			WorkTemplate template, SeriesVersion lineage) {
		return new RecurrenceSeries(id, account, frequency, period, template, lineage);
	}

	public RecurrenceSeries closeAt(long position) {
		return restore(id, organizationId, getFrequency(), getPeriod(), template, lineage.closeAt(position));
	}

	public RecurrenceSeries successor(long position, LocalDate anchor, LocalTime time) {
		var nextPeriod = new RecurrencePeriod(anchor, getPeriod().endsOn());
		var nextTemplate = new WorkTemplate(template.customerId(), template.customerLocationId(), time,
				template.description(), template.contractedHours(), template.hourlyRate(), template.currencyCode(),
				template.participants(), template.initialStatus(), template.frozenPricing());
		return restore(UUID.randomUUID(), organizationId, getFrequency(), nextPeriod, nextTemplate,
				new SeriesVersion(lineage.familyId(), id, position, null));
	}

	public Frequency getFrequency() {
		return calendar.frequency();
	}

	public RecurrencePeriod getPeriod() {
		return calendar.period();
	}

	public long positionOf(LocalDate originalDate) {
		return Math.addExact(lineage.firstPosition(), calendar.indexOf(originalDate));
	}

	public LocalDate dateAt(long position) {
		if (position < lineage.firstPosition()) {
			throw new DomainException("Posição anterior à versão.");
		}
		return calendar.dateAt(position - lineage.firstPosition());
	}

	public List<LocalDate> occurrences(GenerationWindow window) {
		return calendar.occurrences(window).stream().filter(date -> lineage.contains(positionOf(date))).toList();
	}

	private static void validate(UUID id, UUID organizationId, Frequency frequency, RecurrencePeriod period,
			WorkTemplate template) {
		if (id == null || organizationId == null || frequency == null || period == null || template == null) {
			throw new DomainException(
					"Série inválida: frequência, início e condições são obrigatórios; término não pode anteceder o início.");
		}
	}

	private static void validateLineage(UUID id, SeriesVersion lineage) {
		if (lineage == null || id.equals(lineage.previousSeriesId())) {
			throw new DomainException("Linhagem inválida.");
		}
		if (lineage.previousSeriesId() == null) {
			if (!id.equals(lineage.familyId()) || lineage.firstPosition() != 0) {
				throw new DomainException("Linhagem inválida.");
			}
		}
		else if (id.equals(lineage.familyId())) {
			throw new DomainException("Linhagem inválida.");
		}
	}

}
