package com.klaus.moply.workflows.application.usecase.support;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;
import com.klaus.moply.recurrence.domain.ChangeScope;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workflows.application.usecase.dto.OccurrenceSelection;
import com.klaus.moply.workflows.application.usecase.dto.SelectedWork;
import com.klaus.moply.workflows.application.usecase.dto.SelectionContext;
import com.klaus.moply.workorders.application.ports.WorkOrderOccurrences;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;
import com.klaus.moply.workorders.domain.exception.WorkOrderStateException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecurringWorkSelection {

	private final RecurrenceRepository series;

	private final WorkOrderOccurrences occurrences;

	private final WorkOrderOperations operations;

	public SelectionContext lockFamily(Context context, OccurrenceSelection input) {
		var reference = occurrences.reference(context.organizationId(), input.workId());
		if (reference.seriesId() == null) {
			throw new DomainException("Trabalho não recorrente.");
		}
		var versions = series.lockFamily(context.organizationId(), reference.seriesId());
		var anchor = versions.stream()
			.filter(version -> version.getId().equals(reference.seriesId()))
			.findFirst()
			.orElseThrow();
		return new SelectionContext(input, context.organizationId(), anchor,
				anchor.positionOf(reference.originalDate()), versions);
	}

	/**
	 * Call after checking idempotency under the family lock, inside the outer
	 * transaction.
	 */
	public List<SelectedWork> lockOccurrences(SelectionContext family) {
		var positions = selectedPositions(family);
		// All collective commands acquire work order locks in the same order.
		return positions.keySet()
			.stream()
			.sorted(Comparator.comparing(UUID::toString))
			.map(id -> operations.withWorkOrder(family.organizationId(), id,
					work -> new SelectedWork(work, positions.get(id))))
			.toList();
	}

	private Map<UUID, Long> selectedPositions(SelectionContext family) {
		if (family.selection().scope() == ChangeScope.THIS_OCCURRENCE) {
			if (!family.anchor().getLineage().contains(family.fromPosition())) {
				throw new WorkOrderStateException("Ocorrência substituída ou encerrada; consulte o histórico.");
			}
			return Map.of(family.selection().workId(), family.fromPosition());
		}
		var positions = new HashMap<UUID, Long>();
		for (var version : family.versions()) {
			for (var reference : occurrences.inSeries(family.organizationId(), version.getId())) {
				long position = version.positionOf(reference.originalDate());
				if (position >= family.fromPosition() && version.getLineage().contains(position)) {
					positions.put(reference.id(), position);
				}
			}
		}
		return positions;
	}

}
