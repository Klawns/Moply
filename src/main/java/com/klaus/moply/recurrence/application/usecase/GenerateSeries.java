package com.klaus.moply.recurrence.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import com.klaus.moply.accounts.application.exception.AccountNotFoundException;
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
public class GenerateSeries implements Usecase.Contextual<UUID, GenerateSeries.Result> {

	private final RecurrenceRepository repository;

	private final OrganizationRepository accounts;

	private final WorkOrderOccurrences occurrences;

	private final CreateWorkOrder createWork;

	private final Clock clock;

	private final RecurrenceChanges changes;

	public record Result(UUID organizationId, UUID seriesId, LocalDate from, LocalDate until, int created,
			int existing) {
	}

	@Override
	public Result execute(Usecase.Context context, UUID seriesId) {
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
		return new Result(organizationId, seriesId, window.from(), window.until(), created, existing);
	}

	private GenerationWindow generationWindow(UUID organizationId) {
		var account = accounts.findById(organizationId).orElseThrow(AccountNotFoundException::new);
		var today = LocalDate.now(clock.withZone(ZoneId.of(account.timezone())));
		return new GenerationWindow(today);
	}

	private void createOccurrence(Usecase.Context context, RecurrenceSeries series, GenerationWindow window,
			LocalDate date) {
		try {
			var work = createWork.execute(context, toWorkOrderInput(series.getTemplate(), date));
			occurrences.link(context.organizationId(), work.id(), series.getId(), date);
		} catch (RuntimeException error) {
			log.error("recurrence rollback account={} series={} from={} until={} occurrence={}",
					context.organizationId(), series.getId(), window.from(), window.until(), date, error);
			throw error;
		}
	}

	private static CreateWorkOrderInput toWorkOrderInput(WorkTemplate template, LocalDate date) {
		return new CreateWorkOrderInput(template.customerId(), template.customerLocationId(), date,
				template.startTime(), template.description(), template.contractedHours().value(),
				template.hourlyRate().value(), template.participants().ids(), template.initialStatus());
	}

}
