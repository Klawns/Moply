package com.klaus.moply.workflows.application.usecase.dto;

import java.util.List;

public record CancelOccurrenceInput(OccurrenceSelection selection, List<PaymentConfirmation> confirmations) {
	public CancelOccurrenceInput {
		confirmations = confirmations == null ? List.of() : confirmations.stream().toList();
	}
}
