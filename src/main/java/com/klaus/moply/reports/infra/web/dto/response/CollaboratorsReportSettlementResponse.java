package com.klaus.moply.reports.infra.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.shared.infra.web.dto.response.CollaboratorReferenceResponse;
import com.klaus.moply.shared.infra.web.dto.response.CustomerReferenceResponse;

public record CollaboratorsReportSettlementResponse(UUID paymentId, UUID workOrderId,
		CustomerReferenceResponse customer, LocalDate serviceDate, CollaboratorReferenceResponse collaborator,
		LocalDate paidOn, BigDecimal amount) {
}
