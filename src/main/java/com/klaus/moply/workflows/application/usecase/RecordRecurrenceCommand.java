package com.klaus.moply.workflows.application.usecase;

import java.time.Clock;
import java.util.UUID;

import com.klaus.moply.recurrence.application.ports.RecurrenceChanges;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workflows.application.usecase.dto.RecordRecurrenceCommandInput;

import lombok.RequiredArgsConstructor;

/** Records the command in the caller's workflow transaction. */
@RequiredArgsConstructor
public class RecordRecurrenceCommand implements Usecase.Contextual<RecordRecurrenceCommandInput, UUID> {

	private final RecurrenceChanges changes;

	private final Clock clock;

	@Override
	public UUID execute(Usecase.Context context, RecordRecurrenceCommandInput input) {
		var commandId = UUID.randomUUID();
		changes.save(new RecurrenceChanges.Command(commandId, context.organizationId(), input.familyId(),
				input.idempotencyKey(), input.content(), input.actorId(), clock.instant(), input.successorId()));
		return commandId;
	}

}
