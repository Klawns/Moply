package com.klaus.moply.payments.application.usecase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

public record CollaboratorPaymentSummary(UUID collaboratorId, String currencyCode, BigDecimal allocatedAmount,
		BigDecimal recordedAmount, BigDecimal remainingAmount, boolean requiresAttention,
		List<WorkBalance> workOrders) {

	public CollaboratorPaymentSummary {
		workOrders = List.copyOf(workOrders);
	}

	public record WorkBalance(UUID workOrderId, LocalDate serviceDate, WorkOrderStatus status,
			BigDecimal allocatedAmount, BigDecimal recordedAmount, BigDecimal remainingAmount,
			boolean requiresAttention) {
	}

}
