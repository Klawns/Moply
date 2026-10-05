package com.klaus.moply.workflows.infra.transaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.recurrence.application.ports.RecurrenceChanges;
import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workflows.application.usecase.CancelRecurringWork;
import com.klaus.moply.workflows.application.usecase.CancelWorkOrder;
import com.klaus.moply.workflows.application.usecase.CheckRecurrenceCommandReplay;
import com.klaus.moply.workflows.application.usecase.RecordRecurrenceCommand;
import com.klaus.moply.workflows.application.usecase.ValidateRecurringCancellationPayments;
import com.klaus.moply.workflows.application.usecase.dto.CancelOccurrenceInput;
import com.klaus.moply.workflows.application.usecase.support.RecurringWorkSelection;

/** Keeps all workflow effects in a single write transaction. */
@Service
public class TransactionalCancelRecurringWork extends CancelRecurringWork {

	public TransactionalCancelRecurringWork(RecurringWorkSelection selection, CheckRecurrenceCommandReplay replay,
			RecordRecurrenceCommand recordCommand, RecurrenceChanges changes, CancelWorkOrder cancel,
			ValidateRecurringCancellationPayments payments, RecurrenceRepository series) {
		super(selection, replay, recordCommand, changes, cancel, payments, series);
	}

	@Override
	@Transactional
	public Void execute(Usecase.Context context, CancelOccurrenceInput input) {
		return super.execute(context, input);
	}

}
