package com.klaus.moply.workorders.application.usecase;

import java.util.UUID;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workorders.application.ports.WorkOrderOccurrences;
import lombok.RequiredArgsConstructor;

/**
 * Reads immutable occurrence identity without loading operational state before a lock.
 */
@RequiredArgsConstructor
public class FindWorkOccurrence implements Usecase.Contextual<UUID, WorkOrderOccurrences.Reference> {

	private final WorkOrderOccurrences occurrences;

	public WorkOrderOccurrences.Reference execute(Usecase.Context context, UUID id) {
		return occurrences.reference(context.organizationId(), id);
	}

}
