package com.klaus.moply.workflows.application.usecase;

import com.klaus.moply.recurrence.application.ports.RecurrenceChanges;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workflows.application.usecase.dto.CheckRecurrenceCommandReplayInput;
import com.klaus.moply.workorders.domain.exception.WorkOrderStateException;

import lombok.RequiredArgsConstructor;

/** Checks replay while the caller holds the recurrence family lock. */
@RequiredArgsConstructor
public class CheckRecurrenceCommandReplay implements Usecase.Contextual<CheckRecurrenceCommandReplayInput, Boolean> {

	private final RecurrenceChanges changes;

	@Override
	public Boolean execute(Usecase.Context context, CheckRecurrenceCommandReplayInput input) {
		var prior = changes.find(context.organizationId(), input.familyId(), input.idempotencyKey());
		if (prior.isEmpty()) {
			return false;
		}
		if (!prior.get().content().equals(input.content())) {
			throw new WorkOrderStateException("Chave de idempotência reutilizada com conteúdo diferente.");
		}
		return true;
	}

}
