package com.klaus.moply.workflows.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.klaus.moply.recurrence.application.ports.RecurrenceChanges;
import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;
import com.klaus.moply.recurrence.domain.ChangeScope;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workflows.application.usecase.dto.CancelOccurrenceInput;
import com.klaus.moply.workflows.application.usecase.dto.CancelWorkOrderInput;
import com.klaus.moply.workflows.application.usecase.dto.CheckRecurrenceCommandReplayInput;
import com.klaus.moply.workflows.application.usecase.dto.PaymentConfirmation;
import com.klaus.moply.workflows.application.usecase.dto.RecordRecurrenceCommandInput;
import com.klaus.moply.workflows.application.usecase.dto.SelectedWork;
import com.klaus.moply.workflows.application.usecase.dto.SelectionContext;
import com.klaus.moply.workflows.application.usecase.dto.ValidateRecurringCancellationPaymentsInput;
import com.klaus.moply.workflows.application.usecase.support.CancellationConfirmations;
import com.klaus.moply.workflows.application.usecase.support.RecurrenceCommandContent;
import com.klaus.moply.workflows.application.usecase.support.RecurringWorkSelection;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CancelRecurringWork implements Usecase.Contextual<CancelOccurrenceInput, Void> {

	private final RecurringWorkSelection selection;

	private final CheckRecurrenceCommandReplay replay;

	private final RecordRecurrenceCommand recordCommand;

	private final RecurrenceChanges changes;

	private final CancelWorkOrder cancel;

	private final ValidateRecurringCancellationPayments payments;

	private final RecurrenceRepository series;

	@Override
	public Void execute(Usecase.Context context, CancelOccurrenceInput input) {
		validateInput(input);

		var confirmationsByPayment = CancellationConfirmations.from(input.confirmations());
		var content = RecurrenceCommandContent.cancellationContent(input);
		var family = selection.lockFamily(context, input.selection());

		if (replay.execute(context, new CheckRecurrenceCommandReplayInput(family.anchor().getLineage().familyId(),
				family.selection().idempotencyKey(), content))) {
			return null;
		}

		var selectedWorks = selection.lockOccurrences(family);
		var confirmations = matchConfirmations(context, selectedWorks, confirmationsByPayment);
		var commandId = recordCommand.execute(context,
				new RecordRecurrenceCommandInput(family.anchor().getLineage().familyId(),
						family.selection().idempotencyKey(), content, family.selection().actorId(), null));

		cancelOccurrences(context, family, commandId, selectedWorks, confirmations);
		closeSeriesIfNecessary(input, family);

		return null;
	}

	private void validateInput(CancelOccurrenceInput input) {
		if (input == null || input.selection() == null) {
			throw new DomainException("Entrada de cancelamento é obrigatória.");
		}
	}

	private Map<UUID, PaymentConfirmation> matchConfirmations(Usecase.Context context, List<SelectedWork> selectedWorks,
			CancellationConfirmations confirmationsByPayment) {

		var workIds = selectedWorks.stream().map(selected -> selected.work().id()).toList();

		return payments.execute(context,
				new ValidateRecurringCancellationPaymentsInput(workIds, confirmationsByPayment));
	}

	private void cancelOccurrences(Usecase.Context context, SelectionContext family, UUID commandId,
			List<SelectedWork> selectedWorks, Map<UUID, PaymentConfirmation> confirmations) {

		for (var selected : selectedWorks) {
			var confirmation = confirmations.get(selected.work().id());

			cancelOccurrence(context, family, commandId, selected, confirmation);
		}
	}

	private void closeSeriesIfNecessary(CancelOccurrenceInput input, SelectionContext family) {

		if (input.selection().scope() != ChangeScope.THIS_AND_FOLLOWING) {
			return;
		}

		for (var version : family.versions()) {
			series.close(version.closeAt(family.fromPosition()));
		}
	}

	private void cancelOccurrence(Usecase.Context context, SelectionContext family, UUID commandId,
			SelectedWork selected, PaymentConfirmation confirmation) {

		var work = selected.work();

		if (work.status() != WorkOrderStatus.CANCELLED) {
			cancelWorkOrder(context, family, work, confirmation);

			changes.record(new RecurrenceChanges.Item(UUID.randomUUID(), context.organizationId(), commandId,
					selected.work().id(), selected.position(), "CANCELLED", null, selected.work().serviceDate(),
					work.serviceDate()));
		}

		changes.exclude(context.organizationId(), family.anchor().getLineage().familyId(), selected.position(),
				selected.work().id());
	}

	private void cancelWorkOrder(Usecase.Context context, SelectionContext family, WorkOrder work,
			PaymentConfirmation confirmation) {

		var hasConfirmation = confirmation != null;
		var reason = hasConfirmation ? confirmation.reason() : null;

		var input = new CancelWorkOrderInput(work.id(), family.selection().actorId(), hasConfirmation, reason);

		cancel.execute(context, input);
	}

}
