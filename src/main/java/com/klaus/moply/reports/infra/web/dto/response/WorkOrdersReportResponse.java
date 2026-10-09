package com.klaus.moply.reports.infra.web.dto.response;

import com.klaus.moply.reports.application.usecase.dto.WorkOrdersReport;
import com.klaus.moply.shared.infra.web.dto.PageResponse;
import com.klaus.moply.shared.infra.web.dto.response.CustomerReferenceResponse;

public record WorkOrdersReportResponse(ReportPeriodResponse period, ReportContextResponse context,
		WorkOrdersReportSummaryResponse summary, PageResponse<WorkOrdersReportWorkResponse> works) {

	public static WorkOrdersReportResponse from(WorkOrdersReport report) {
		return new WorkOrdersReportResponse(new ReportPeriodResponse(report.from(), report.to()),
				new ReportContextResponse(report.timezone(), report.currencyCode(), report.referenceDate()),
				new WorkOrdersReportSummaryResponse(report.realizedAmount(), report.realizedPendingAmount(),
						report.workProjectionAmount()),
				PageResponse.from(report.works(), w -> new WorkOrdersReportWorkResponse(w.workOrderId(),
						new CustomerReferenceResponse(w.customerId(), w.customerName()), w.serviceDate(), w.status(),
						w.totalAmount(), w.realized(), w.hasActivePayment(), w.pendingFromCustomer())));
	}

}
