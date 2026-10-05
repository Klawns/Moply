package com.klaus.moply.recurrence.application.usecase;

import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.recurrence.application.ports.RecurrenceChanges;
import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;
import com.klaus.moply.recurrence.application.usecase.dto.OccurrenceHistoryOutput;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workorders.application.ports.WorkOrderOccurrences;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindOccurrenceHistory
		implements Usecase.Contextual<FindOccurrenceHistory.Filter, PageResult<OccurrenceHistoryOutput>> {

	private final RecurrenceChanges changes;

	private final RecurrenceRepository series;

	private final WorkOrderOccurrences occurrences;

	@Override
	public PageResult<OccurrenceHistoryOutput> execute(Usecase.Context context, Filter input) {
		var organizationId = context.organizationId();
		var workOrderId = input.workOrderId();
		occurrences.reference(organizationId, workOrderId);
		return changes.history(organizationId, workOrderId, input.page()).map(item -> toOutput(organizationId, item));
	}

	private OccurrenceHistoryOutput toOutput(UUID organizationId, RecurrenceChanges.Item item) {
		var command = changes.command(organizationId, item.command()).orElseThrow();
		LocalDate targetOccurrenceDate = null;
		UUID replacementWorkId = null;
		if (item.targetSeries() != null) {
			var targetSeries = series.find(organizationId, item.targetSeries()).orElseThrow();
			targetOccurrenceDate = targetSeries.dateAt(item.position());
			replacementWorkId = findReplacementWorkId(organizationId, targetSeries.getId(), targetOccurrenceDate);
		}

		return new OccurrenceHistoryOutput(command.id(), command.actor(), command.at(), item.reason(), item.position(),
				item.targetSeries(), targetOccurrenceDate, replacementWorkId, item.serviceDateBefore(),
				item.serviceDateAfter());
	}

	private UUID findReplacementWorkId(UUID organizationId, UUID seriesId, LocalDate occurrenceDate) {
		return occurrences.inSeries(organizationId, seriesId)
			.stream()
			.filter(occurrence -> occurrence.originalDate().equals(occurrenceDate))
			.map(WorkOrderOccurrences.Reference::id)
			.findFirst()
			.orElse(null);
	}

	public record Filter(UUID workOrderId, PageQuery page) {
	}

}
