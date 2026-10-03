package com.klaus.moply.workflows.infra;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workflows.application.CancelWorkOrder;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;

/** Owns the outer transaction for future cross-module cancellation effects. */
@Service
public class TransactionalCancelWorkOrder extends CancelWorkOrder {

	public TransactionalCancelWorkOrder(WorkOrderOperations operations, WorkOrderPaymentRepository payments,
			CollaboratorPaymentRepository collaboratorPayments, Clock clock) {
		super(operations, payments, collaboratorPayments, clock);
	}

	@Override
	@Transactional
	public Void execute(Usecase.Context context, UUID id) {
		return super.execute(context, id);
	}

	@Override
	@Transactional
	public Void execute(Usecase.Context context, Input input) {
		return super.execute(context, input);
	}

}
