package com.klaus.moply.workflows.infra.transaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workflows.application.usecase.CancelWorkOrder;
import com.klaus.moply.workflows.application.usecase.ReversePaymentForWorkOrderCancellation;
import com.klaus.moply.workflows.application.usecase.dto.CancelWorkOrderInput;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;

/** Keeps all workflow effects in a single write transaction. */
@Service
public class TransactionalCancelWorkOrder extends CancelWorkOrder {

	public TransactionalCancelWorkOrder(WorkOrderOperations operations,
			ReversePaymentForWorkOrderCancellation payments) {
		super(operations, payments);
	}

	@Override
	@Transactional
	public Void execute(Usecase.Context context, CancelWorkOrderInput input) {
		return super.execute(context, input);
	}

}
