package com.klaus.moply.workflows.infra;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.accounts.application.ports.OrganizationRepository;
import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;

@Service
public class TransactionalRescheduleWorkOrder extends com.klaus.moply.workflows.application.RescheduleWorkOrder {

	public TransactionalRescheduleWorkOrder(WorkOrderOperations operations, WorkOrderPaymentRepository payments,
			OrganizationRepository organizations, Clock clock) {
		super(operations, payments, organizations, clock);
	}

	@Override
	@Transactional
	public Void execute(com.klaus.moply.shared.application.usecase.Usecase.Context context, Input input) {
		return super.execute(context, input);
	}

}
