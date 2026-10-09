package com.klaus.moply.payments.application.usecase;

import java.time.Clock;

import com.klaus.moply.payments.application.usecase.dto.ReverseCollaboratorPaymentInput;
import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.payments.application.usecase.exception.PaymentNotFoundException;
import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.exception.ApplicationException;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReverseCollaboratorPayment implements Usecase.Contextual<ReverseCollaboratorPaymentInput, Payment> {

	private final WorkOrderOperations workOrders;

	private final CollaboratorPaymentRepository payments;

	private final Clock clock;

	@Override
	public Payment execute(Usecase.Context context, ReverseCollaboratorPaymentInput input) {
		validate(input);
		var organizationId = context.organizationId();
		var paymentId = input.paymentId();
		var initial = payments.findById(organizationId, paymentId)
			.orElseThrow(() -> new PaymentNotFoundException(paymentId));
		if (!initial.workOrderId().equals(input.workOrderId())
				|| !initial.collaboratorId().equals(input.collaboratorId()))
			throw new PaymentNotFoundException(paymentId);
		var workOrderId = initial.workOrderId();
		return workOrders.withWorkOrder(organizationId, workOrderId, work -> {
			var entry = payments.findById(organizationId, paymentId)
				.orElseThrow(() -> new PaymentNotFoundException(paymentId));
			if (!entry.workOrderId().equals(input.workOrderId())
					|| !entry.collaboratorId().equals(input.collaboratorId()))
				throw new PaymentNotFoundException(paymentId);
			if (entry.payment().status() == Payment.Status.REVERSED)
				return entry.payment();
			if (work.status() == WorkOrderStatus.CANCELLED)
				throw new ApplicationException("Acerto de trabalho cancelado não pode ser revertido.");
			var reversed = entry.payment().reverse(input.actorId(), input.reason(), clock.instant());
			payments.update(reversed);
			return reversed;
		});
	}

	private void validate(ReverseCollaboratorPaymentInput input) {
		if (input == null || input.workOrderId() == null || input.collaboratorId() == null || input.paymentId() == null
				|| input.actorId() == null)
			throw new ApplicationException("Acerto e responsável são obrigatórios.");
		if (!input.confirmNotActuallyPaid())
			throw new ApplicationException("Confirme que o valor não foi efetivamente entregue ao colaborador.");
		if (input.reason() == null || input.reason().isBlank())
			throw new ApplicationException("Motivo da reversão é obrigatório.");
	}

}
