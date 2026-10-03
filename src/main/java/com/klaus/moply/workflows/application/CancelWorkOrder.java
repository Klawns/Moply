package com.klaus.moply.workflows.application;

import java.time.Clock;
import java.util.UUID;

import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.payments.application.usecase.exception.PaymentConflictException;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;

import lombok.RequiredArgsConstructor;

/** Coordinates cancellation through the public work contract. */
@RequiredArgsConstructor
public class CancelWorkOrder implements Usecase.Contextual<UUID, Void> {

	private final WorkOrderOperations operations;

	private final WorkOrderPaymentRepository payments;

	private final CollaboratorPaymentRepository collaboratorPayments;

	private final Clock clock;

	public record Input(UUID id, UUID actorId, boolean confirmNoMoneyReceived, String reason) {
	}

	@Override
	public Void execute(Usecase.Context context, UUID id) {
		return execute(context, new Input(id, null, false, null));
	}

	public Void execute(Usecase.Context context, Input input) {
		operations.update(context.organizationId(), input.id(), work -> {
			if (collaboratorPayments.hasRecordedForWork(context.organizationId(), work.id()))
				throw new PaymentConflictException("Trabalho com acerto ativo de colaborador não pode ser cancelado.");
			var active = payments.findActiveByWork(context.organizationId(), work.id());
			if (active.isPresent()) {
				if (!input.confirmNoMoneyReceived())
					throw new PaymentConflictException(
							"Confirme que o dinheiro não foi recebido para reverter o pagamento.");
				if (input.actorId() == null || input.reason() == null || input.reason().isBlank())
					throw new DomainException("Responsável e motivo da reversão são obrigatórios.");
				payments.update(active.get().reverse(input.actorId(), input.reason(), clock.instant()));
			}
			return work.cancel();
		});
		return null;
	}

}
