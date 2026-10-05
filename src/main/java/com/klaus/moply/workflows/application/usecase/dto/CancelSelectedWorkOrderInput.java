package com.klaus.moply.workflows.application.usecase.dto;

import java.util.List;
import java.util.UUID;

import com.klaus.moply.recurrence.domain.ChangeScope;

public record CancelSelectedWorkOrderInput(UUID workId, UUID actorId, Options options) {
	public record Options(Boolean confirmNoMoneyReceived, String reason, ChangeScope scope, String idempotencyKey,
			List<PaymentConfirmation> confirmations) {
	}
}