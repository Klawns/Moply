package com.klaus.moply.workorders.infra.web.dto.request;

import java.util.List;

import com.klaus.moply.recurrence.domain.ChangeScope;
import com.klaus.moply.workflows.application.usecase.dto.CancelSelectedWorkOrderInput.Options;

public record CancelRequest(Boolean confirmNoMoneyReceived, String reason, ChangeScope scope, String idempotencyKey,
		List<ReversalConfirmationRequest> confirmations) {
	public Options toOptions() {
		return new Options(confirmNoMoneyReceived, reason, scope, idempotencyKey,
				confirmations == null ? null
						: confirmations.stream()
							.map(confirmation -> confirmation == null ? null : confirmation.toInput())
							.toList());
	}
}
