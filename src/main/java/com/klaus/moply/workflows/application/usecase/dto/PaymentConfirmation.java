package com.klaus.moply.workflows.application.usecase.dto;

import java.util.UUID;

public record PaymentConfirmation(UUID paymentId, boolean confirmNoMoneyReceived, String reason) {
	public PaymentConfirmation {
		reason = reason == null ? null : reason.strip();
	}
}
