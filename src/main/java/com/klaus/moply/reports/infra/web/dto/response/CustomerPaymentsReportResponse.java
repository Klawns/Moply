package com.klaus.moply.reports.infra.web.dto.response;

import com.klaus.moply.reports.application.usecase.dto.CustomerPaymentsReport;
import com.klaus.moply.shared.infra.web.dto.PageResponse;
import com.klaus.moply.shared.infra.web.dto.response.CustomerReferenceResponse;

public record CustomerPaymentsReportResponse(ReportPeriodResponse period, ReportContextResponse context,
		CustomerPaymentsReportSummaryResponse summary, PageResponse<CustomerPaymentsReportPaymentResponse> payments) {

	public static CustomerPaymentsReportResponse from(CustomerPaymentsReport report) {
		return new CustomerPaymentsReportResponse(new ReportPeriodResponse(report.from(), report.to()),
				new ReportContextResponse(report.timezone(), report.currencyCode(), null),
				new CustomerPaymentsReportSummaryResponse(report.totalAmount()),
				PageResponse.from(report.payments(), p -> new CustomerPaymentsReportPaymentResponse(p.paymentId(),
						p.workOrderId(), new CustomerReferenceResponse(p.customerId(), p.customerName()), p.paidOn(),
						p.amount())));
	}

}
