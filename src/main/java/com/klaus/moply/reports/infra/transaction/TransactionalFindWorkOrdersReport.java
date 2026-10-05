package com.klaus.moply.reports.infra.transaction;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.reports.application.ports.ReportReadRepository;
import com.klaus.moply.reports.application.usecase.FindWorkOrdersReport;
import com.klaus.moply.reports.application.usecase.ReportContextResolver;
import com.klaus.moply.reports.application.usecase.dto.ReportPeriod;
import com.klaus.moply.reports.application.usecase.dto.WorkOrdersReport;
import com.klaus.moply.shared.application.usecase.Usecase;

public class TransactionalFindWorkOrdersReport extends FindWorkOrdersReport {

	public TransactionalFindWorkOrdersReport(ReportReadRepository reports, ReportContextResolver contextResolver) {
		super(reports, contextResolver);
	}

	@Override
	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public WorkOrdersReport execute(Usecase.Context context, ReportPeriod input) {
		return super.execute(context, input);
	}

}
