package com.klaus.moply.workflows.application.usecase;

import com.klaus.moply.accounts.application.usecase.GetOrganizationDate;
import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.payments.application.usecase.exception.PaymentConflictException;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workflows.application.usecase.dto.RescheduleWorkOrderInput;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RescheduleWorkOrder implements Usecase.Contextual<RescheduleWorkOrderInput, Void> {

	private final WorkOrderOperations operations;

	private final WorkOrderPaymentRepository payments;

	private final GetOrganizationDate organizationDate;

	@Override
	public Void execute(Usecase.Context context, RescheduleWorkOrderInput input) {
		validateInput(input);
		var organizationId = context.organizationId();

		var today = organizationDate.execute(context, null);

		operations.update(organizationId, input.id(), work -> {
			if (payments.findActiveByWork(organizationId, work.id()).isPresent()) {
				throw new PaymentConflictException("Trabalho pago não pode ser reagendado.");
			}

			return work.reschedule(input.serviceDate(), input.startTime(), today);
		});

		return null;
	}

	private void validateInput(RescheduleWorkOrderInput input) {
		if (input == null) {
			throw new DomainException("Trabalho e data obrigatórios.");
		}
	}

}
