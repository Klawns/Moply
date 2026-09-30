package com.klaus.moply.workflows.infra;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workflows.application.CancelWorkOrder;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;

/** Owns the outer transaction for future cross-module cancellation effects. */
@Service
public class TransactionalCancelWorkOrder extends CancelWorkOrder {

	public TransactionalCancelWorkOrder(WorkOrderOperations operations) {
		super(operations);
	}

	@Override
	@Transactional
	public Void execute(Usecase.Context context, UUID id) {
		return super.execute(context, id);
	}

}
