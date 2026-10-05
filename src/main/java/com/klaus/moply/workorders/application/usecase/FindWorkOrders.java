package com.klaus.moply.workorders.application.usecase;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.dto.WorkOrderOutput;
import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.vo.WorkOrderDateRange;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindWorkOrders implements Usecase.Contextual<FindWorkOrders.Filter, PageResult<WorkOrderOutput>> {

	private final WorkOrderRepository repo;

	private final CustomerRepository customers;

	public record Filter(WorkOrderDateRange dateRange, UUID customerId, WorkOrderStatus status, PageQuery page) {
		public Filter(LocalDate from, LocalDate to, UUID customerId) {
			this(from, to, customerId, null, PageQuery.defaults());
		}

		public Filter(LocalDate from, LocalDate to, UUID customerId, WorkOrderStatus status) {
			this(new WorkOrderDateRange(from, to), customerId, status, PageQuery.defaults());
		}

		public Filter(LocalDate from, LocalDate to, UUID customerId, WorkOrderStatus status, PageQuery page) {
			this(new WorkOrderDateRange(from, to), customerId, status, page);
		}

		public Filter {
			Objects.requireNonNull(dateRange);
			Objects.requireNonNull(page);
		}
	}

	public PageResult<WorkOrderOutput> execute(Usecase.Context context, Filter input) {
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
