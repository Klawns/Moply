package com.klaus.moply.workorders.application.usecase;

import java.util.UUID;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.dto.WorkOrderOutput;
import com.klaus.moply.workorders.application.usecase.exception.WorkOrderNotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindWorkOrderById implements Usecase.Contextual<UUID, WorkOrderOutput> {

	private final WorkOrderRepository repo;

	private final CustomerRepository customers;

	public WorkOrderOutput execute(Usecase.Context context, UUID id) {
		var work = repo.findById(context.organizationId(), id).orElseThrow(() -> new WorkOrderNotFoundException(id));
		var customer = customers.findById(context.organizationId(), work.customerId())
			.orElseThrow(() -> new CustomerNotFoundException(work.customerId()));
		return WorkOrderOutput.from(work, customer.getName().value());
	}

}
