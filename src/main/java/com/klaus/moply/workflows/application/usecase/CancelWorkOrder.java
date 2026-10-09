package com.klaus.moply.workflows.application.usecase;

import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.exception.ApplicationException;
import com.klaus.moply.workflows.application.usecase.dto.CancelWorkOrderInput;
import com.klaus.moply.workorders.application.ports.WorkOrderOperations;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CancelWorkOrder implements Usecase.Contextual<CancelWorkOrderInput, Void> {

	private final WorkOrderOperations operations;

	private final ReversePaymentForWorkOrderCancellation payments;

	@Override
	public Void execute(Usecase.Context context, CancelWorkOrderInput input) {
		validateInput(input);
		operations.update(context.organizationId(), input.id(), work -> {
			payments.execute(context, input);
			// The adapter persists the immutable entity returned by the transition.
			return work.cancel();
		});
		return null;
	}

	private void validateInput(CancelWorkOrderInput input) {
		if (input == null) {
			throw new ApplicationException("Trabalho obrigatório.");
		}
	}

}
