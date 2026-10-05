package com.klaus.moply.workorders.infra.web.dto.request;

import java.util.UUID;

import com.klaus.moply.workflows.application.usecase.dto.PaymentConfirmation;

public record ReversalConfirmationRequest(UUID paymentId, boolean confirmNoMoneyReceived, String reason) {
	public PaymentConfirmation toInput() {
		return new PaymentConfirmation(paymentId, confirmNoMoneyReceived, reason);
	}
}
