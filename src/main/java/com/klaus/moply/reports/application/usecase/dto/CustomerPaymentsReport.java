package com.klaus.moply.reports.application.usecase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.shared.application.pagination.PageResult;

public record CustomerPaymentsReport(LocalDate from, LocalDate to, String timezone, String currencyCode,
		BigDecimal totalAmount, PageResult<Payment> payments) {

	public record Payment(UUID paymentId, UUID workOrderId, UUID customerId, String customerName, LocalDate paidOn,
			BigDecimal amount) {
	}
}
