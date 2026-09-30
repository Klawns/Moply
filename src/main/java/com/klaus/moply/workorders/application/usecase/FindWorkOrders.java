package com.klaus.moply.workorders.application.usecase;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.dto.WorkOrderOutput;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindWorkOrders implements Usecase.Contextual<FindWorkOrders.Filter, List<WorkOrderOutput>> {

	private final WorkOrderRepository repo;

	private final CustomerRepository customers;

	public record Filter(LocalDate from, LocalDate to, UUID customerId, WorkOrderStatus status) {
		public Filter(LocalDate from, LocalDate to, UUID customerId) {
			this(from, to, customerId, null);
		}

		public Filter {
			if (from != null && to != null && from.isAfter(to))
				throw new DomainException("Intervalo de datas invertido.");
		}
	}

	public List<WorkOrderOutput> execute(Usecase.Context context, Filter input) {
		return repo.findAll(context.organizationId(), input.from(), input.to(), input.customerId(), input.status())
			.stream()
			.map(w -> {
				var customer = customers.findById(context.organizationId(), w.customerId())
					.orElseThrow(() -> new CustomerNotFoundException(w.customerId()));
				return WorkOrderOutput.from(w, customer.getName().value());
			})
			.toList();
	}

}
