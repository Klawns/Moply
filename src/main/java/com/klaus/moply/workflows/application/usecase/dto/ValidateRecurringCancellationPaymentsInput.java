package com.klaus.moply.workflows.application.usecase.dto;

import java.util.List;
import java.util.UUID;

import com.klaus.moply.workflows.application.usecase.support.CancellationConfirmations;

public record ValidateRecurringCancellationPaymentsInput(List<UUID> workOrderIds,
		CancellationConfirmations confirmations) {
	public ValidateRecurringCancellationPaymentsInput {
		workOrderIds = List.copyOf(workOrderIds);
	}
}
