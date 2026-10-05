package com.klaus.moply.reports.application.usecase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.shared.application.pagination.PageResult;

public record WorkOrdersReport(LocalDate from, LocalDate to, LocalDate referenceDate, String timezone,
		String currencyCode, BigDecimal realizedAmount, BigDecimal realizedPendingAmount,
		BigDecimal workProjectionAmount, PageResult<Work> works) {

	public record Work(UUID workOrderId, UUID customerId, String customerName, LocalDate serviceDate, String status,
			BigDecimal totalAmount, boolean realized, boolean hasActivePayment, boolean pendingFromCustomer) {
	}
}
