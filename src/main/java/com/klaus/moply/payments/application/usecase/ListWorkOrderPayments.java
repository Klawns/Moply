package com.klaus.moply.payments.application.usecase;

import com.klaus.moply.payments.application.usecase.dto.ListWorkOrderPaymentsInput;
import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.usecase.exception.ApplicationException;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.exception.WorkOrderNotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListWorkOrderPayments implements Usecase.Contextual<ListWorkOrderPaymentsInput, PageResult<Payment>> {

	private final WorkOrderPaymentRepository payments;

	private final WorkOrderRepository workOrders;

	@Override
	public PageResult<Payment> execute(Usecase.Context context, ListWorkOrderPaymentsInput input) {
		validate(input);

		workOrders.findById(context.organizationId(), input.workOrderId())
			.orElseThrow(() -> new WorkOrderNotFoundException(input.workOrderId()));
		return payments.findAllByWork(context.organizationId(), input.workOrderId(), input.page());
	}

	private void validate(ListWorkOrderPaymentsInput input) {
		if (input == null || input.workOrderId() == null)
			throw new ApplicationException("Trabalho é obrigatório.");
	}

}
