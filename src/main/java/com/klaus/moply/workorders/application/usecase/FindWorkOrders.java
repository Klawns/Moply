package com.klaus.moply.workorders.application.usecase;

import com.klaus.moply.workorders.application.usecase.dto.FindWorkOrdersFilter;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.dto.WorkOrderOutput;
import com.klaus.moply.workorders.domain.entity.WorkOrder;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindWorkOrders implements Usecase.Contextual<FindWorkOrdersFilter, PageResult<WorkOrderOutput>> {

	private final WorkOrderRepository repo;

	private final CustomerRepository customers;

	public PageResult<WorkOrderOutput> execute(Usecase.Context context, FindWorkOrdersFilter input) {
		return repo
			.search(context.organizationId(), input.dateRange(), input.customerId(), input.status(), input.page())
			.map(work -> toOutput(context, work));
	}

	private WorkOrderOutput toOutput(Usecase.Context context, WorkOrder work) {
		var customer = customers.findById(context.organizationId(), work.customerId())
			.orElseThrow(() -> new CustomerNotFoundException(work.customerId()));
		return WorkOrderOutput.from(work, customer.getName().value());
	}

}
