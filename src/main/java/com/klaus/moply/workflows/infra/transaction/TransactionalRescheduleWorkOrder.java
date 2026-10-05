package com.klaus.moply.workflows.infra.transaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.accounts.application.usecase.GetOrganizationDate;
import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workflows.application.usecase.RescheduleWorkOrder;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleWorkOrderInput;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;

/** Keeps all workflow effects in a single write transaction. */
@Service
public class TransactionalRescheduleWorkOrder extends RescheduleWorkOrder {

	public TransactionalRescheduleWorkOrder(WorkOrderOperations operations, WorkOrderPaymentRepository payments,
			GetOrganizationDate organizationDate) {
		super(operations, payments, organizationDate);
	}

	@Override
	@Transactional
	public Void execute(Usecase.Context context, RescheduleWorkOrderInput input) {
		return super.execute(context, input);
	}

}
