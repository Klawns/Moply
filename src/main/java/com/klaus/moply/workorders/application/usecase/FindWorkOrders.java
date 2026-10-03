package com.klaus.moply.workorders.application.usecase;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.dto.WorkOrderOutput;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.vo.WorkOrderDateRange;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindWorkOrders implements Usecase.Contextual<FindWorkOrders.Filter, List<WorkOrderOutput>> {

	private final WorkOrderRepository repo;

	private final CustomerRepository customers;

	public record Filter(WorkOrderDateRange dateRange, UUID customerId, WorkOrderStatus status) {
		public Filter(LocalDate from, LocalDate to, UUID customerId) {
			this(from, to, customerId, null);
		}

		public Filter(LocalDate from, LocalDate to, UUID customerId, WorkOrderStatus status) {
			this(new WorkOrderDateRange(from, to), customerId, status);
		}

		public Filter {
			Objects.requireNonNull(dateRange);
		}
	}

	public List<WorkOrderOutput> execute(Usecase.Context context, Filter input) {
		return repo.findAll(context.organizationId(), input.dateRange(), input.customerId(), input.status())
			.stream()
			.map(work -> toOutput(context, work))
			.toList();
	}

	private WorkOrderOutput toOutput(Usecase.Context context, WorkOrder work) {
		var customer = customers.findById(context.organizationId(), work.customerId())
			.orElseThrow(() -> new CustomerNotFoundException(work.customerId()));
		return WorkOrderOutput.from(work, customer.getName().value());
	}

}
