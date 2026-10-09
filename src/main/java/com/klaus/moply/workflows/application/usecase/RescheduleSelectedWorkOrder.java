package com.klaus.moply.workflows.application.usecase;

import com.klaus.moply.recurrence.domain.ChangeScope;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.exception.ApplicationException;
import com.klaus.moply.workflows.application.usecase.dto.OccurrenceSelection;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleOccurrenceInput;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleSelectedWorkOrderInput;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleWorkOrderInput;
import com.klaus.moply.workorders.application.usecase.FindWorkOccurrence;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RescheduleSelectedWorkOrder implements Usecase.Contextual<RescheduleSelectedWorkOrderInput, Void> {

	private final FindWorkOccurrence occurrence;

	private final RescheduleWorkOrder standalone;

	private final RescheduleRecurringWork recurring;

	@Override
	public Void execute(Usecase.Context context, RescheduleSelectedWorkOrderInput input) {
		if (occurrence.execute(context, input.workId()).seriesId() != null) {
			return recurring.execute(context, new RescheduleOccurrenceInput(
					new OccurrenceSelection(input.workId(), input.actorId(), input.scope(), input.idempotencyKey()),
					input.serviceDate(), input.startTime()));
		}
		if (input.scope() == ChangeScope.THIS_AND_FOLLOWING)
			throw new ApplicationException("Trabalho avulso não possui próximas ocorrências.");
		return standalone.execute(context,
				new RescheduleWorkOrderInput(input.workId(), input.serviceDate(), input.startTime()));
	}

}
