package com.klaus.moply.reports.infra.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.shared.infra.web.dto.response.CustomerReferenceResponse;

public record CustomerPaymentsReportPaymentResponse(UUID paymentId, UUID workOrderId,
		CustomerReferenceResponse customer, LocalDate paidOn, BigDecimal amount) {
}
