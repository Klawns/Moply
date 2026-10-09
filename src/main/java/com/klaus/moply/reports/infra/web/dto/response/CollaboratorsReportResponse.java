package com.klaus.moply.reports.infra.web.dto.response;

import com.klaus.moply.reports.application.usecase.dto.CollaboratorsReport;
import com.klaus.moply.shared.infra.web.dto.PageResponse;
import com.klaus.moply.shared.infra.web.dto.response.CollaboratorReferenceResponse;
import com.klaus.moply.shared.infra.web.dto.response.CustomerReferenceResponse;

public record CollaboratorsReportResponse(ReportPeriodResponse period, ReportContextResponse context,
		CollaboratorsReportSummaryResponse summary, PageResponse<CollaboratorsReportAssignmentResponse> assignments,
		PageResponse<CollaboratorsReportSettlementResponse> settlements) {

	public static CollaboratorsReportResponse from(CollaboratorsReport report) {
		return new CollaboratorsReportResponse(new ReportPeriodResponse(report.from(), report.to()),
				new ReportContextResponse(report.timezone(), report.currencyCode(), report.referenceDate()),
				new CollaboratorsReportSummaryResponse(report.allocatedTotal(), report.realizedAllocatedTotal(),
						report.futureAllocatedTotal(), report.pendingTotal(), report.realizedPendingTotal(),
						report.futurePendingTotal(), report.settlementsOnPeriodTotal()),
				PageResponse.from(report.assignments(), a -> new CollaboratorsReportAssignmentResponse(a.workOrderId(),
						new CustomerReferenceResponse(a.customerId(), a.customerName()), a.serviceDate(),
						a.workStatus(), new CollaboratorReferenceResponse(a.collaboratorId(), a.collaboratorName()),
						a.allocatedAmount(), a.activeSettlements(), a.pendingAmount(), a.realized())),
				PageResponse.from(report.settlements(),
						s -> new CollaboratorsReportSettlementResponse(s.paymentId(), s.workOrderId(),
								new CustomerReferenceResponse(s.customerId(), s.customerName()), s.serviceDate(),
								new CollaboratorReferenceResponse(s.collaboratorId(), s.collaboratorName()), s.paidOn(),
								s.amount())));
	}

}
