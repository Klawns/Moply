package com.klaus.moply.reports.infra.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.reports.application.usecase.dto.CollaboratorsReport;
import com.klaus.moply.shared.infra.web.dto.PageResponse;

public record CollaboratorsReportResponse(LocalDate from, LocalDate to, LocalDate referenceDate, String timezone,
		String currencyCode, BigDecimal allocatedTotal, BigDecimal realizedAllocatedTotal,
		BigDecimal futureAllocatedTotal, BigDecimal pendingTotal, BigDecimal realizedPendingTotal,
		BigDecimal futurePendingTotal, BigDecimal settlementsOnPeriodTotal, PageResponse<Assignment> assignments,
		PageResponse<Settlement> settlements) {

	public static CollaboratorsReportResponse from(CollaboratorsReport report) {
		return new CollaboratorsReportResponse(report.from(), report.to(), report.referenceDate(), report.timezone(),
				report.currencyCode(), report.allocatedTotal(), report.realizedAllocatedTotal(),
				report.futureAllocatedTotal(), report.pendingTotal(), report.realizedPendingTotal(),
				report.futurePendingTotal(), report.settlementsOnPeriodTotal(),
				PageResponse.from(report.assignments(),
						a -> new Assignment(a.workOrderId(), a.customerId(), a.customerName(), a.serviceDate(),
								a.workStatus(), a.collaboratorId(), a.collaboratorName(), a.allocatedAmount(),
								a.activeSettlements(), a.pendingAmount(), a.realized())),
				PageResponse.from(report.settlements(),
						s -> new Settlement(s.paymentId(), s.workOrderId(), s.customerId(), s.customerName(),
								s.serviceDate(), s.collaboratorId(), s.collaboratorName(), s.paidOn(), s.amount())));
	}

	public record Assignment(UUID workOrderId, UUID customerId, String customerName, LocalDate serviceDate,
			String workStatus, UUID collaboratorId, String collaboratorName, BigDecimal allocatedAmount,
			BigDecimal activeSettlements, BigDecimal pendingAmount, boolean realized) {
	}

	public record Settlement(UUID paymentId, UUID workOrderId, UUID customerId, String customerName,
			LocalDate serviceDate, UUID collaboratorId, String collaboratorName, LocalDate paidOn, BigDecimal amount) {
	}
}
