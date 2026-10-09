package com.klaus.moply.payments.application.usecase.dto;

import java.util.UUID;

public record ReverseWorkOrderPaymentInput(UUID paymentId, boolean confirmNoMoneyReceived, String reason,
		UUID actorId) {
}
