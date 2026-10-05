package com.klaus.moply.workflows.application.usecase;

import java.util.List;
import java.util.UUID;

import com.klaus.moply.accounts.application.usecase.GetOrganizationDate;
import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.payments.application.usecase.exception.PaymentConflictException;
import com.klaus.moply.recurrence.application.ports.RecurrenceChanges;
import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;
import com.klaus.moply.recurrence.application.usecase.GenerateSeries;
import com.klaus.moply.recurrence.domain.ChangeScope;
import com.klaus.moply.recurrence.domain.RecurrenceSeries;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workflows.application.usecase.dto.CancelWorkOrderInput;
import com.klaus.moply.workflows.application.usecase.dto.CheckRecurrenceCommandReplayInput;
import com.klaus.moply.workflows.application.usecase.dto.RecordRecurrenceCommandInput;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleOccurrenceInput;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleWorkOrderInput;
import com.klaus.moply.workflows.application.usecase.dto.SelectedWork;
import com.klaus.moply.workflows.application.usecase.dto.SelectionContext;
import com.klaus.moply.workflows.application.usecase.support.RecurrenceCommandContent;
import com.klaus.moply.workflows.application.usecase.support.RecurringWorkSelection;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.exception.WorkOrderStateException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RescheduleRecurringWork implements Usecase.Contextual<RescheduleOccurrenceInput, Void> {

	private final RecurringWorkSelection selection;

	private final CheckRecurrenceCommandReplay replay;

	private final RecordRecurrenceCommand recordCommand;

	private final RecurrenceChanges changes;

	private final RescheduleWorkOrder reschedule;

	private final CancelWorkOrder cancel;

	private final RecurrenceRepository series;

	private final GenerateSeries generate;

	private final WorkOrderPaymentRepository payments;

	private final CollaboratorPaymentRepository collaboratorPayments;

	private final GetOrganizationDate organizationDate;

	@Override
	public Void execute(Usecase.Context context, RescheduleOccurrenceInput input) {
		validateInput(input);

		var content = RecurrenceCommandContent.reschedulingContent(input);
		var family = selection.lockFamily(context, input.selection());
		if (replay.execute(context, new CheckRecurrenceCommandReplayInput(family.anchor().getLineage().familyId(),
				family.selection().idempotencyKey(), content))) {
			return null;
		}
		var selectedWorks = selection.lockOccurrences(family);
		if (input.selection().scope() == ChangeScope.THIS_OCCURRENCE) {
			rescheduleOccurrence(context, input, family, selectedWorks.getFirst(), content);
		}
		else {
			replaceFollowingOccurrences(context, input, family, selectedWorks, content);
		}
		return null;
	}

	private void validateInput(RescheduleOccurrenceInput input) {
		if (input == null) {
			throw new DomainException("Seleção e nova data são obrigatórias.");
		}
	}

	private void rescheduleOccurrence(Usecase.Context context, RescheduleOccurrenceInput input, SelectionContext family,
			SelectedWork selected, String content) {
		reschedule.execute(context,
				new RescheduleWorkOrderInput(input.selection().workId(), input.serviceDate(), input.startTime()));
		var commandId = recordCommand.execute(context,
				new RecordRecurrenceCommandInput(family.anchor().getLineage().familyId(),
						family.selection().idempotencyKey(), content, family.selection().actorId(), null));
		changes.record(
				new RecurrenceChanges.Item(UUID.randomUUID(), context.organizationId(), commandId, selected.work().id(),
						selected.position(), "RESCHEDULED", null, selected.work().serviceDate(), input.serviceDate()));
	}

	private void replaceFollowingOccurrences(Usecase.Context context, RescheduleOccurrenceInput input,
			SelectionContext family, List<SelectedWork> selectedWorks, String content) {
		var successor = createSuccessor(family, input);
		validateReplacement(context, selectedWorks, successor);
		for (var version : family.versions()) {
			series.close(version.closeAt(family.fromPosition()));
		}
		series.save(successor);
		var commandId = recordCommand.execute(context,
				new RecordRecurrenceCommandInput(family.anchor().getLineage().familyId(),
						family.selection().idempotencyKey(), content, family.selection().actorId(), successor.getId()));
		for (var selected : selectedWorks) {
			replaceOccurrence(context, family, commandId, selected, successor.getId());
		}
		generate.execute(context, successor.getId());
	}

	private RecurrenceSeries createSuccessor(SelectionContext family, RescheduleOccurrenceInput input) {
		if (family.versions().stream().noneMatch(version -> version.getLineage().contains(family.fromPosition()))) {
			throw new WorkOrderStateException("Esta parte da família já foi encerrada.");
		}
		var successor = family.anchor().successor(family.fromPosition(), input.serviceDate(), input.startTime());
		// Empty historical intervals must not extend a terminal cancellation.
		var activeIntervals = family.versions()
			.stream()
			.map(version -> version.getLineage())
			.filter(interval -> interval.untilPosition() == null || interval.untilPosition() > interval.firstPosition())
			.toList();
		if (activeIntervals.stream().allMatch(interval -> interval.untilPosition() != null)) {
			long untilPosition = activeIntervals.stream()
				.mapToLong(interval -> interval.untilPosition())
				.max()
				.orElseThrow();
			return successor.closeAt(untilPosition);
		}
		return successor;
	}

	private void validateReplacement(Usecase.Context context, List<SelectedWork> selectedWorks,
			RecurrenceSeries successor) {
		var today = organizationDate.execute(context, null);
		for (var selected : selectedWorks) {
			var work = selected.work();
			if (work.status() != WorkOrderStatus.CANCELLED) {
				requireReplacementAllowed(context.organizationId(), work.id());
				// Validate the target date without moving the historical work order.
				work.reschedule(successor.dateAt(selected.position()), successor.getTemplate().startTime(), today);
			}
		}
	}

	private void requireReplacementAllowed(UUID organizationId, UUID workOrderId) {
		if (payments.findActiveByWork(organizationId, workOrderId).isPresent()) {
			throw new PaymentConflictException("Trabalho pago não pode ser reagendado.");
		}
		if (collaboratorPayments.hasRecordedForWork(organizationId, workOrderId)) {
			throw new PaymentConflictException("Acerto ativo impede o cancelamento para substituição.");
		}
	}

	private void replaceOccurrence(Usecase.Context context, SelectionContext family, UUID commandId,
			SelectedWork selected, UUID successorId) {
		if (selected.work().status() == WorkOrderStatus.CANCELLED) {
			changes.exclude(context.organizationId(), family.anchor().getLineage().familyId(), selected.position(),
					selected.work().id());
		}
		else {
			cancel.execute(context,
					new CancelWorkOrderInput(selected.work().id(), family.selection().actorId(), false, null));
			changes.record(new RecurrenceChanges.Item(UUID.randomUUID(), context.organizationId(), commandId,
					selected.work().id(), selected.position(), "REPLACED", successorId, selected.work().serviceDate(),
					selected.work().serviceDate()));
		}
	}

}
