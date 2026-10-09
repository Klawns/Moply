package com.klaus.moply.payments.application.usecase;

import java.time.Clock;

import com.klaus.moply.payments.application.usecase.dto.ReverseWorkOrderPaymentInput;
import com.klaus.moply.payments.application.ports.WorkOrderPaymentRepository;
import com.klaus.moply.payments.application.usecase.exception.PaymentNotFoundException;
import com.klaus.moply.payments.domain.Payment;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.exception.ApplicationException;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReverseWorkOrderPayment implements Usecase.Contextual<ReverseWorkOrderPaymentInput, Payment> {

	private final WorkOrderOperations workOrders;

	private final WorkOrderPaymentRepository payments;

	private final Clock clock;

	@Override
	public Payment execute(Usecase.Context context, ReverseWorkOrderPaymentInput input) {
		validate(input);

		var organizationId = context.organizationId();
		var paymentId = input.paymentId();

		var workOrderId = payments.findWorkOrderIdByPayment(organizationId, paymentId)
			.orElseThrow(() -> new PaymentNotFoundException(paymentId));

		return workOrders.withWorkOrder(organizationId, workOrderId, ignoredWorkOrder -> {
			var payment = payments.findById(organizationId, paymentId)
				.orElseThrow(() -> new PaymentNotFoundException(paymentId));
			if (payment.status() != Payment.Status.RECORDED)
				return payment;

			var reversed = payment.reverse(input.actorId(), input.reason(), clock.instant());
			payments.update(reversed);
			return reversed;
		});
	}

	private void validate(ReverseWorkOrderPaymentInput input) {
		if (input == null || input.paymentId() == null || input.actorId() == null) {
			throw new ApplicationException("Pagamento e responsável são obrigatórios.");
		}

		if (!input.confirmNoMoneyReceived()) {
			throw new ApplicationException("Confirme que o dinheiro não foi efetivamente recebido.");
		}

		if (input.reason() == null || input.reason().isBlank()) {
			throw new ApplicationException("Motivo da reversão é obrigatório.");
		}
	}

}
