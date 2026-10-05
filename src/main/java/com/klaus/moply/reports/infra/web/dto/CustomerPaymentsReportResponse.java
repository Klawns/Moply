package com.klaus.moply.reports.infra.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.reports.application.usecase.dto.CustomerPaymentsReport;
import com.klaus.moply.shared.infra.web.dto.PageResponse;

public record CustomerPaymentsReportResponse(LocalDate from, LocalDate to, String timezone, String currencyCode,
		BigDecimal totalAmount, PageResponse<Payment> payments) {

	public static CustomerPaymentsReportResponse from(CustomerPaymentsReport report) {
		return new CustomerPaymentsReportResponse(report.from(), report.to(), report.timezone(), report.currencyCode(),
				report.totalAmount(), PageResponse.from(report.payments(), p -> new Payment(p.paymentId(),
						p.workOrderId(), p.customerId(), p.customerName(), p.paidOn(), p.amount())));
	}

	public record Payment(UUID paymentId, UUID workOrderId, UUID customerId, String customerName, LocalDate paidOn,
			BigDecimal amount) {
	}
}
