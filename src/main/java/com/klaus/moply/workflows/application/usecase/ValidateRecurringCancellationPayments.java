package com.klaus.moply.workflows.application.usecase;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.klaus.moply.payments.application.ports.CollaboratorPaymentRepository;
import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.payments.application.usecase.exception.PaymentConflictException;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.workflows.application.usecase.dto.PaymentConfirmation;
import com.klaus.moply.workflows.application.usecase.dto.ValidateRecurringCancellationPaymentsInput;

import lombok.RequiredArgsConstructor;

/** Validates active payments after the caller locks all selected work orders. */
@RequiredArgsConstructor
public class ValidateRecurringCancellationPayments
		implements Usecase.Contextual<ValidateRecurringCancellationPaymentsInput, Map<UUID, PaymentConfirmation>> {

	private final WorkOrderPaymentRepository payments;

	private final CollaboratorPaymentRepository collaboratorPayments;

	@Override
	public Map<UUID, PaymentConfirmation> execute(Usecase.Context context,
			ValidateRecurringCancellationPaymentsInput input) {
		var organizationId = context.organizationId();
		var remaining = new HashMap<>(input.confirmations().byPayment());
		var byWorkOrder = new HashMap<UUID, PaymentConfirmation>();
		for (var workOrderId : input.workOrderIds()) {
			requireNoSettlement(organizationId, workOrderId,
					"Trabalho com acerto ativo de colaborador não pode ser cancelado.");
			payments.findActiveByWork(organizationId, workOrderId).ifPresent(payment -> {
				var confirmation = remaining.remove(payment.id());
				if (confirmation == null) {
					throw new PaymentConflictException(
							"Confirme individualmente a ausência de recebimento do pagamento " + payment.id());
				}
				byWorkOrder.put(workOrderId, confirmation);
			});
		}
		if (!remaining.isEmpty()) {
			throw new PaymentConflictException("Confirmação não corresponde a pagamento ativo do conjunto.");
		}
		return Map.copyOf(byWorkOrder);
	}

	private void requireNoSettlement(UUID organizationId, UUID workOrderId, String message) {
		if (collaboratorPayments.hasRecordedForWork(organizationId, workOrderId)) {
			throw new PaymentConflictException(message);
		}
	}

}
