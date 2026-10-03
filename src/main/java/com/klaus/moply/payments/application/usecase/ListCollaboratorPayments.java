package com.klaus.moply.payments.application.usecase;

import java.util.List;
import java.util.UUID;

import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.ports.WorkOrderRepository;
import com.klaus.moply.workorders.application.usecase.exception.WorkOrderNotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListCollaboratorPayments implements Usecase.Contextual<ListCollaboratorPayments.Input, List<Payment>> {

	private final CollaboratorPaymentRepository payments;

	private final WorkOrderRepository workOrders;

	public record Input(UUID workOrderId, UUID collaboratorId) {
	}

	@Override
	public List<Payment> execute(Usecase.Context context, Input input) {
		if (input == null || input.workOrderId() == null || input.collaboratorId() == null)
			throw new DomainException("Trabalho e colaborador são obrigatórios.");
		var work = workOrders.findById(context.organizationId(), input.workOrderId())
			.orElseThrow(() -> new WorkOrderNotFoundException(input.workOrderId()));
		if (work.assignments().stream().noneMatch(a -> a.collaboratorId().equals(input.collaboratorId())))
			throw new DomainException("Colaborador não participa deste trabalho.");
		return payments.findAll(context.organizationId(), input.workOrderId(), input.collaboratorId());
	}

}
