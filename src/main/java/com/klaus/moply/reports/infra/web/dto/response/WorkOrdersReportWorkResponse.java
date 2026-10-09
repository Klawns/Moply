package com.klaus.moply.reports.infra.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.shared.infra.web.dto.response.CustomerReferenceResponse;

public record WorkOrdersReportWorkResponse(UUID workOrderId, CustomerReferenceResponse customer, LocalDate serviceDate,
		String status, BigDecimal totalAmount, boolean realized, boolean hasActivePayment,
		boolean pendingFromCustomer) {
}
