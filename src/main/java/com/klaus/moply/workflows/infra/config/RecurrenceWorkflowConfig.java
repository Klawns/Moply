package com.klaus.moply.workflows.infra.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.recurrence.application.ports.RecurrenceChanges;
import com.klaus.moply.recurrence.application.ports.RecurrenceRepository;
import com.klaus.moply.workflows.application.usecase.CancelRecurringWork;
import com.klaus.moply.workflows.application.usecase.CancelSelectedWorkOrder;
import com.klaus.moply.workflows.application.usecase.CancelWorkOrder;
import com.klaus.moply.workflows.application.usecase.CheckRecurrenceCommandReplay;
import com.klaus.moply.workflows.application.usecase.RecordRecurrenceCommand;
import com.klaus.moply.workflows.application.usecase.RescheduleRecurringWork;
import com.klaus.moply.workflows.application.usecase.RescheduleSelectedWorkOrder;
import com.klaus.moply.workflows.application.usecase.RescheduleWorkOrder;
import com.klaus.moply.workflows.application.usecase.ReversePaymentForWorkOrderCancellation;
import com.klaus.moply.workflows.application.usecase.ValidateRecurringCancellationPayments;
import com.klaus.moply.workflows.application.usecase.support.RecurringWorkSelection;
import com.klaus.moply.workorders.application.ports.WorkOrderOccurrences;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;
import com.klaus.moply.workorders.application.usecase.FindWorkOccurrence;

@Configuration
public class RecurrenceWorkflowConfig {

	@Bean
	CancelSelectedWorkOrder cancelSelectedWorkOrder(FindWorkOccurrence occurrence, CancelWorkOrder standalone,
			CancelRecurringWork recurring) {
		return new CancelSelectedWorkOrder(occurrence, standalone, recurring);
	}

	@Bean
	RescheduleSelectedWorkOrder rescheduleSelectedWorkOrder(FindWorkOccurrence occurrence,
			RescheduleWorkOrder standalone, RescheduleRecurringWork recurring) {
		return new RescheduleSelectedWorkOrder(occurrence, standalone, recurring);
	}

	@Bean
	RecurringWorkSelection recurringWorkSelection(RecurrenceRepository series, WorkOrderOccurrences occurrences,
			WorkOrderOperations operations) {
		return new RecurringWorkSelection(series, occurrences, operations);
	}

	@Bean
	CheckRecurrenceCommandReplay checkRecurrenceCommandReplay(RecurrenceChanges changes) {
		return new CheckRecurrenceCommandReplay(changes);
	}

	@Bean
	RecordRecurrenceCommand recordRecurrenceCommand(RecurrenceChanges changes, Clock clock) {
		return new RecordRecurrenceCommand(changes, clock);
	}

	@Bean
	ReversePaymentForWorkOrderCancellation reversePaymentForWorkOrderCancellation(WorkOrderPaymentRepository payments,
			CollaboratorPaymentRepository collaboratorPayments, Clock clock) {
		return new ReversePaymentForWorkOrderCancellation(payments, collaboratorPayments, clock);
	}

	@Bean
	ValidateRecurringCancellationPayments validateRecurringCancellationPayments(WorkOrderPaymentRepository payments,
			CollaboratorPaymentRepository collaboratorPayments) {
		return new ValidateRecurringCancellationPayments(payments, collaboratorPayments);
	}

}
