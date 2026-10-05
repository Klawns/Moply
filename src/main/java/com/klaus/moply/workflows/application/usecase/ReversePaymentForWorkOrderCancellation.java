package com.klaus.moply.workflows.application.usecase;

import java.time.Clock;
import java.util.UUID;

import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.payments.application.usecase.exception.PaymentConflictException;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workflows.application.usecase.dto.CancelWorkOrderInput;

import lombok.RequiredArgsConstructor;

/** Reverses payment inside the caller's transaction and work order lock. */
@RequiredArgsConstructor
public class ReversePaymentForWorkOrderCancellation implements Usecase.Contextual<CancelWorkOrderInput, Void> {

	private final WorkOrderPaymentRepository payments;

	private final CollaboratorPaymentRepository collaboratorPayments;

	private final Clock clock;

	@Override
	public Void execute(Usecase.Context context, CancelWorkOrderInput input) {
		var organizationId = context.organizationId();
		requireNoSettlement(organizationId, input.id(),
				"Trabalho com acerto ativo de colaborador não pode ser cancelado.");
		var activePayment = payments.findActiveByWork(organizationId, input.id());
		if (activePayment.isEmpty()) {
			return null;
		}
		if (!input.confirmNoMoneyReceived()) {
			throw new PaymentConflictException("Confirme que o dinheiro não foi recebido para reverter o pagamento.");
		}
		if (input.actorId() == null || input.reason() == null || input.reason().isBlank()) {
			throw new DomainException("Responsável e motivo da reversão são obrigatórios.");
		}
		payments.update(activePayment.get().reverse(input.actorId(), input.reason(), clock.instant()));
		return null;
	}

	private void requireNoSettlement(UUID organizationId, UUID workOrderId, String message) {
		if (collaboratorPayments.hasRecordedForWork(organizationId, workOrderId)) {
			throw new PaymentConflictException(message);
		}
	}

}
