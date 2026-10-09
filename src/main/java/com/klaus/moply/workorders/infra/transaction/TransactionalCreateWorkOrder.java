package com.klaus.moply.workorders.infra.transaction;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import com.klaus.moply.workorders.application.service.WorkOrderPreparation;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.CreateWorkOrder;
import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;
import com.klaus.moply.workorders.application.usecase.dto.WorkOrderOutput;
import com.klaus.moply.workorders.domain.vo.WorkOrderPricing;
import com.klaus.moply.workorders.domain.vo.WorkOrderAssignments;

public class TransactionalCreateWorkOrder extends CreateWorkOrder {

	public TransactionalCreateWorkOrder(WorkOrderRepository repo, CustomerRepository customers,
			WorkOrderPreparation preparation) {
		super(repo, customers, preparation);
	}

	@Override
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public WorkOrderOutput execute(Usecase.Context context, CreateWorkOrderInput input) {
		return super.execute(context, input);
	}

	@Override
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public WorkOrderOutput executeFrozen(Usecase.Context context, CreateWorkOrderInput input, WorkOrderPricing pricing,
			WorkOrderAssignments assignments) {
		return super.executeFrozen(context, input, pricing, assignments);
	}

}
