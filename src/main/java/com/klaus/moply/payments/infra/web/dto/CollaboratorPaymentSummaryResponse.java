package com.klaus.moply.payments.infra.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.klaus.moply.payments.application.usecase.CollaboratorPaymentSummary;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

public record CollaboratorPaymentSummaryResponse(UUID collaboratorId, String currencyCode, BigDecimal allocatedAmount,
		BigDecimal recordedAmount, BigDecimal remainingAmount, boolean requiresAttention,
		List<WorkBalance> workOrders) {

	public static CollaboratorPaymentSummaryResponse from(CollaboratorPaymentSummary summary) {
		return new CollaboratorPaymentSummaryResponse(summary.collaboratorId(), summary.currencyCode(),
				summary.allocatedAmount(), summary.recordedAmount(), summary.remainingAmount(),
				summary.requiresAttention(), summary.workOrders().stream().map(WorkBalance::from).toList());
	}

	public record WorkBalance(UUID workOrderId, LocalDate serviceDate, WorkOrderStatus status,
			BigDecimal allocatedAmount, BigDecimal recordedAmount, BigDecimal remainingAmount,
			boolean requiresAttention) {

		static WorkBalance from(CollaboratorPaymentSummary.WorkBalance balance) {
			return new WorkBalance(balance.workOrderId(), balance.serviceDate(), balance.status(),
					balance.allocatedAmount(), balance.recordedAmount(), balance.remainingAmount(),
					balance.requiresAttention());
		}

	}

}
