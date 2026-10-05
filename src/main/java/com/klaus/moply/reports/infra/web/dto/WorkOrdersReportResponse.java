package com.klaus.moply.reports.infra.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.reports.application.usecase.dto.WorkOrdersReport;
import com.klaus.moply.shared.infra.web.dto.PageResponse;

public record WorkOrdersReportResponse(LocalDate from, LocalDate to, LocalDate referenceDate, String timezone,
		String currencyCode, BigDecimal realizedAmount, BigDecimal realizedPendingAmount,
		BigDecimal workProjectionAmount, PageResponse<Work> works) {

	public static WorkOrdersReportResponse from(WorkOrdersReport report) {
		return new WorkOrdersReportResponse(report.from(), report.to(), report.referenceDate(), report.timezone(),
				report.currencyCode(), report.realizedAmount(), report.realizedPendingAmount(),
				report.workProjectionAmount(),
				PageResponse.from(report.works(),
						w -> new Work(w.workOrderId(), w.customerId(), w.customerName(), w.serviceDate(), w.status(),
								w.totalAmount(), w.realized(), w.hasActivePayment(), w.pendingFromCustomer())));
	}

	public record Work(UUID workOrderId, UUID customerId, String customerName, LocalDate serviceDate, String status,
			BigDecimal totalAmount, boolean realized, boolean hasActivePayment, boolean pendingFromCustomer) {
	}
}
