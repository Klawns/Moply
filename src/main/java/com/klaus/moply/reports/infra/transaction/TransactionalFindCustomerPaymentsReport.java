package com.klaus.moply.reports.infra.transaction;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.reports.application.ports.ReportReadRepository;
import com.klaus.moply.reports.application.usecase.FindCustomerPaymentsReport;
import com.klaus.moply.reports.application.usecase.ReportContextResolver;
import com.klaus.moply.reports.application.usecase.dto.CustomerPaymentsReport;
import com.klaus.moply.reports.application.usecase.dto.ReportPeriod;
import com.klaus.moply.shared.application.usecase.Usecase;

public class TransactionalFindCustomerPaymentsReport extends FindCustomerPaymentsReport {

	public TransactionalFindCustomerPaymentsReport(ReportReadRepository reports,
			ReportContextResolver contextResolver) {
		super(reports, contextResolver);
	}

	@Override
	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public CustomerPaymentsReport execute(Usecase.Context context, ReportPeriod input) {
		return super.execute(context, input);
	}

}
