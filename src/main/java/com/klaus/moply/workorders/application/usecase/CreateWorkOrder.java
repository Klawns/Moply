package com.klaus.moply.workorders.application.usecase;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.service.WorkOrderPreparation;
import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;
import com.klaus.moply.workorders.application.usecase.dto.WorkOrderOutput;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.vo.WorkOrderAssignments;
import com.klaus.moply.workorders.domain.vo.WorkOrderPricing;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateWorkOrder implements Usecase.Contextual<CreateWorkOrderInput, WorkOrderOutput> {

	private final WorkOrderRepository repo;

	private final CustomerRepository customers;

	private final WorkOrderPreparation preparation;

	@Override
	public WorkOrderOutput execute(Usecase.Context context, CreateWorkOrderInput input) {
		var prepared = preparation.prepare(context, input);
		return save(context, prepared);
	}

	private WorkOrderOutput save(Usecase.Context context, WorkOrder work) {
		var saved = repo.save(context.organizationId(), work);
		var customer = customers.findById(context.organizationId(), work.customerId())
			.orElseThrow(() -> new CustomerNotFoundException(work.customerId()));
		return WorkOrderOutput.from(saved, customer.getName().value());
	}

	/** Recurrences use approved frozen conditions, never the current tariffs. */
	public WorkOrderOutput executeFrozen(Usecase.Context context, CreateWorkOrderInput input, WorkOrderPricing pricing,
			WorkOrderAssignments assignments) {
		var work = preparation.prepareFrozen(context, input, pricing, assignments);
		return save(context, work);
	}

}
