package com.klaus.moply.reports.infra.transaction;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.reports.application.ports.ReportReadRepository;
import com.klaus.moply.reports.application.usecase.FindCollaboratorsReport;
import com.klaus.moply.reports.application.usecase.ReportContextResolver;
import com.klaus.moply.reports.application.usecase.dto.CollaboratorsReport;
import com.klaus.moply.reports.application.usecase.dto.CollaboratorsReportInput;
import com.klaus.moply.shared.application.usecase.Usecase;

public class TransactionalFindCollaboratorsReport extends FindCollaboratorsReport {

	public TransactionalFindCollaboratorsReport(ReportReadRepository reports, ReportContextResolver contextResolver) {
		super(reports, contextResolver);
	}

	@Override
	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public CollaboratorsReport execute(Usecase.Context context, CollaboratorsReportInput input) {
		return super.execute(context, input);
	}

}
