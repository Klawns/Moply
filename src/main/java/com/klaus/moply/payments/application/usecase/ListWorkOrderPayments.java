package com.klaus.moply.payments.application.usecase;

import java.util.UUID;

import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.exception.WorkOrderNotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListWorkOrderPayments implements Usecase.Contextual<ListWorkOrderPayments.Input, PageResult<Payment>> {

	private final WorkOrderPaymentRepository payments;

	private final WorkOrderRepository workOrders;

	public record Input(UUID workOrderId, PageQuery page) {
		public Input(UUID workOrderId) {
			this(workOrderId, PageQuery.defaults());
		}
	}

	@Override
	public PageResult<Payment> execute(Usecase.Context context, Input input) {
		validate(input);

		workOrders.findById(context.organizationId(), input.workOrderId())
			.orElseThrow(() -> new WorkOrderNotFoundException(input.workOrderId()));
		return payments.findAllByWork(context.organizationId(), input.workOrderId(), input.page());
	}

	private void validate(Input input) {
		if (input == null || input.workOrderId() == null)
			throw new DomainException("Trabalho é obrigatório.");
	}

}
