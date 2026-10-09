package com.klaus.moply.reports.infra.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.shared.infra.web.dto.response.CollaboratorReferenceResponse;
import com.klaus.moply.shared.infra.web.dto.response.CustomerReferenceResponse;

public record CollaboratorsReportAssignmentResponse(UUID workOrderId, CustomerReferenceResponse customer,
		LocalDate serviceDate, String workStatus, CollaboratorReferenceResponse collaborator,
		BigDecimal allocatedAmount, BigDecimal activeSettlements, BigDecimal pendingAmount, boolean realized) {
}
