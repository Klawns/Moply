package com.klaus.moply.workflows.infra.transaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.accounts.application.usecase.GetOrganizationDate;
import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.recurrence.application.ports.RecurrenceChanges;
import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;
import com.klaus.moply.recurrence.application.usecase.GenerateSeries;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workflows.application.usecase.CancelWorkOrder;
import com.klaus.moply.workflows.application.usecase.CheckRecurrenceCommandReplay;
import com.klaus.moply.workflows.application.usecase.RecordRecurrenceCommand;
import com.klaus.moply.workflows.application.usecase.RescheduleRecurringWork;
import com.klaus.moply.workflows.application.usecase.RescheduleWorkOrder;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleOccurrenceInput;
import com.klaus.moply.workflows.application.usecase.support.RecurringWorkSelection;

/** Keeps all workflow effects in a single write transaction. */
@Service
public class TransactionalRescheduleRecurringWork extends RescheduleRecurringWork {

	public TransactionalRescheduleRecurringWork(RecurringWorkSelection selection, CheckRecurrenceCommandReplay replay,
			RecordRecurrenceCommand recordCommand, RecurrenceChanges changes, RescheduleWorkOrder reschedule,
			CancelWorkOrder cancel, RecurrenceRepository series, GenerateSeries generate,
			WorkOrderPaymentRepository payments, CollaboratorPaymentRepository collaboratorPayments,
			GetOrganizationDate organizationDate) {
		super(selection, replay, recordCommand, changes, reschedule, cancel, series, generate, payments,
				collaboratorPayments, organizationDate);
	}

	@Override
	@Transactional
	public Void execute(Usecase.Context context, RescheduleOccurrenceInput input) {
		return super.execute(context, input);
	}

}
