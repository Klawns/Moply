package com.klaus.moply.recurrence.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import com.klaus.moply.recurrence.application.usecase.dto.GenerateSeriesResult;
import com.klaus.moply.accounts.application.usecase.exception.AccountNotFoundException;
import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.recurrence.application.ports.RecurrenceChanges;
import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;
import com.klaus.moply.recurrence.domain.RecurrenceSeries;
import com.klaus.moply.recurrence.domain.WorkTemplate;
import com.klaus.moply.recurrence.domain.vo.GenerationWindow;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workorders.application.ports.WorkOrderOccurrences;
import com.klaus.moply.workorders.application.usecase.CreateWorkOrder;
import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GenerateSeries implements Usecase.Contextual<UUID, GenerateSeriesResult> {

	private final RecurrenceRepository repository;

	private final OrganizationRepository accounts;

	private final WorkOrderOccurrences occurrences;

	private final CreateWorkOrder createWork;

	private final Clock clock;

	private final RecurrenceChanges changes;

	@Override
	public GenerateSeriesResult execute(Usecase.Context context, UUID seriesId) {
		var organizationId = context.organizationId();
		var series = repository.lock(organizationId, seriesId);
		var window = generationWindow(organizationId);
		var occurrenceDates = series.occurrences(window);
		var existingDates = occurrences.findDates(organizationId, seriesId, window.from(), window.until());
		var familyId = series.getLineage().familyId();

		int created = 0;
		int existing = 0;
		for (var date : occurrenceDates) {
			if (existingDates.contains(date) || changes.excluded(organizationId, familyId, series.positionOf(date))) {
				existing++;
				continue;
			}
			createOccurrence(context, series, window, date);
			created++;
		}
		return new GenerateSeriesResult(organizationId, seriesId, window.from(), window.until(), created, existing);
	}

	private GenerationWindow generationWindow(UUID organizationId) {
		var account = accounts.findById(organizationId).orElseThrow(AccountNotFoundException::new);
		var today = LocalDate.now(clock.withZone(ZoneId.of(account.timezone())));
		return new GenerationWindow(today);
	}

	private void createOccurrence(Usecase.Context context, RecurrenceSeries series, GenerationWindow window,
			LocalDate date) {
		try {
			var frozen = series.getTemplate().frozenPricing();
			var work = createWork.executeFrozen(context, toWorkOrderInput(series.getTemplate(), date),
					frozen == null ? null : frozen.pricing(), frozen == null ? null : frozen.assignments());
			occurrences.link(context.organizationId(), work.id(), series.getId(), date);
		}
		catch (RuntimeException error) {
			log.atError()
				.addKeyValue("event", "recurrence.rollback")
				.addKeyValue("accountId", context.organizationId())
				.addKeyValue("seriesId", series.getId())
				.addKeyValue("from", window.from())
				.addKeyValue("until", window.until())
				.addKeyValue("occurrence", date)
				.setCause(error)
				.log("Falha ao gerar ocorrência de recorrência; transação revertida");
			throw error;
		}
	}

	private static CreateWorkOrderInput toWorkOrderInput(WorkTemplate template, LocalDate date) {
		return new CreateWorkOrderInput(template.customerId(), template.customerLocationId(), date,
				template.startTime(), template.description(), template.contractedHours().value(),
				template.hourlyRate().value(), template.participants().ids(), template.initialStatus());
	}

}
