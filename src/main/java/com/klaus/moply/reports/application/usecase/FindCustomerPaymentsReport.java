package com.klaus.moply.reports.application.usecase;

import java.math.BigDecimal;

import com.klaus.moply.reports.application.ports.ReportReadRepository;
import com.klaus.moply.reports.application.usecase.dto.CustomerPaymentsReport;
import com.klaus.moply.reports.application.usecase.dto.ReportPeriod;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.usecase.Usecase;

public class FindCustomerPaymentsReport implements Usecase.Contextual<ReportPeriod, CustomerPaymentsReport> {

	private final ReportReadRepository reports;

	private final ReportContextResolver contextResolver;

	public FindCustomerPaymentsReport(ReportReadRepository reports, ReportContextResolver contextResolver) {
		this.reports = reports;
		this.contextResolver = contextResolver;
	}

	@Override
	public CustomerPaymentsReport execute(
			Usecase.Context context,
			ReportPeriod period) {

		var reportContext = contextResolver.resolve(context, period, null);
		var payments = findPayments(context, period);
		var total = findTotal(context, period);

		return new CustomerPaymentsReport(
				period.from(),
				period.to(),
				reportContext.timezone(),
				reportContext.currencyCode(),
				total,
				payments);
	}

	private PageResult<CustomerPaymentsReport.Payment> findPayments(
			Usecase.Context context,
			ReportPeriod period) {

		return reports.customerPayments(
				context.organizationId(),
				period.from(),
				period.to(),
				period.customerId(),
				period.page())
				.map(this::toPayment);
	}

	private BigDecimal findTotal(
			Usecase.Context context,
			ReportPeriod period) {

		return reports.customerPaymentTotal(
				context.organizationId(),
				period.from(),
				period.to(),
				period.customerId());
	}

	private CustomerPaymentsReport.Payment toPayment(
			ReportReadRepository.PaymentRow row) {

		return new CustomerPaymentsReport.Payment(
				row.paymentId(),
				row.workOrderId(),
				row.customerId(),
				row.customerName(),
				row.paidOn(),
				row.amount());
	}

}
