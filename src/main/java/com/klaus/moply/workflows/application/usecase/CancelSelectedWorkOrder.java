package com.klaus.moply.workflows.application.usecase;

import com.klaus.moply.recurrence.domain.ChangeScope;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workflows.application.usecase.dto.CancelOccurrenceInput;
import com.klaus.moply.workflows.application.usecase.dto.CancelSelectedWorkOrderInput;
import com.klaus.moply.workflows.application.usecase.dto.CancelWorkOrderInput;
import com.klaus.moply.workflows.application.usecase.dto.OccurrenceSelection;
import com.klaus.moply.workorders.application.usecase.FindWorkOccurrence;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CancelSelectedWorkOrder implements Usecase.Contextual<CancelSelectedWorkOrderInput, Void> {

	private final FindWorkOccurrence occurrence;

	private final CancelWorkOrder standalone;

	private final CancelRecurringWork recurring;

	@Override
	public Void execute(Usecase.Context context, CancelSelectedWorkOrderInput input) {
		var options = input.options();
		if (occurrence.execute(context, input.workId()).seriesId() != null) {
			if (options == null)
				throw new DomainException("Alcance e chave são obrigatórios para recorrências.");
			return recurring.execute(context, new CancelOccurrenceInput(
					new OccurrenceSelection(input.workId(), input.actorId(), options.scope(), options.idempotencyKey()),
					options.confirmations()));
		}
		if (options != null && options.scope() == ChangeScope.THIS_AND_FOLLOWING)
			throw new DomainException("Trabalho avulso não possui próximas ocorrências.");
		return standalone.execute(context,
				new CancelWorkOrderInput(input.workId(), input.actorId(),
						options != null && Boolean.TRUE.equals(options.confirmNoMoneyReceived()),
						options == null ? null : options.reason()));
	}

}
