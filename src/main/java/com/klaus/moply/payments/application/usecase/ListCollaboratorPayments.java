package com.klaus.moply.payments.application.usecase;

import java.util.UUID;

import com.klaus.moply.payments.application.usecase.dto.ListCollaboratorPaymentsInput;
import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.exception.WorkOrderNotFoundException;
import com.klaus.moply.workorders.domain.entity.WorkOrder;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListCollaboratorPayments
		implements Usecase.Contextual<ListCollaboratorPaymentsInput, PageResult<Payment>> {

	private final CollaboratorPaymentRepository payments;

	private final WorkOrderRepository workOrders;

	@Override
	public PageResult<Payment> execute(Usecase.Context context, ListCollaboratorPaymentsInput input) {
		validateInput(input);

		var workOrder = findWorkOrder(context, input.workOrderId());

		validateCollaboratorAssignment(workOrder, input.collaboratorId());

		return payments.findAll(context.organizationId(), input.workOrderId(), input.collaboratorId(), input.page());
	}

	private void validateInput(ListCollaboratorPaymentsInput input) {
		if (input == null || input.workOrderId() == null || input.collaboratorId() == null) {
			throw new DomainException("Trabalho e colaborador são obrigatórios.");
		}
	}

	private WorkOrder findWorkOrder(Usecase.Context context, UUID workOrderId) {
		return workOrders.findById(context.organizationId(), workOrderId)
			.orElseThrow(() -> new WorkOrderNotFoundException(workOrderId));
	}

	private void validateCollaboratorAssignment(WorkOrder workOrder, UUID collaboratorId) {
		var isAssigned = workOrder.assignments()
			.stream()
			.anyMatch(assignment -> assignment.collaboratorId().equals(collaboratorId));

		if (!isAssigned) {
			throw new DomainException("Colaborador não participa deste trabalho.");
		}
	}

}
