package com.klaus.moply.reports.application.usecase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.shared.application.pagination.PageResult;

public record CollaboratorsReport(LocalDate from, LocalDate to, LocalDate referenceDate, String timezone,
		String currencyCode, BigDecimal allocatedTotal, BigDecimal realizedAllocatedTotal,
		BigDecimal futureAllocatedTotal, BigDecimal pendingTotal, BigDecimal realizedPendingTotal,
		BigDecimal futurePendingTotal, BigDecimal settlementsOnPeriodTotal, PageResult<Assignment> assignments,
		PageResult<Settlement> settlements) {

	public record Assignment(UUID workOrderId, UUID customerId, String customerName, LocalDate serviceDate,
			String workStatus, UUID collaboratorId, String collaboratorName, BigDecimal allocatedAmount,
			BigDecimal activeSettlements, BigDecimal pendingAmount, boolean realized) {
	}

	public record Settlement(UUID paymentId, UUID workOrderId, UUID customerId, String customerName,
			LocalDate serviceDate, UUID collaboratorId, String collaboratorName, LocalDate paidOn, BigDecimal amount) {
	}
}
