package com.klaus.moply.reports.application.usecase;

import java.time.LocalDate;

import com.klaus.moply.reports.application.ports.ReportReadRepository;
import com.klaus.moply.reports.application.usecase.ReportContextResolver.ReportContext;
import com.klaus.moply.reports.application.usecase.dto.ReportPeriod;
import com.klaus.moply.reports.application.usecase.dto.WorkOrdersReport;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.usecase.Usecase;

public class FindWorkOrdersReport implements Usecase.Contextual<ReportPeriod, WorkOrdersReport> {

	private final ReportReadRepository reports;

	private final ReportContextResolver contextResolver;

	public FindWorkOrdersReport(ReportReadRepository reports, ReportContextResolver contextResolver) {
		this.reports = reports;
		this.contextResolver = contextResolver;
	}

	@Override
	public WorkOrdersReport execute(Usecase.Context context, ReportPeriod period) {

		var reportContext = contextResolver.resolve(context, period, null);
		var works = findWorks(context, period, reportContext.referenceDate());
		var totals = findTotals(context, period, reportContext.referenceDate());

		return createReport(period, reportContext, totals, works);
	}

	private PageResult<WorkOrdersReport.Work> findWorks(Usecase.Context context, ReportPeriod period,
			LocalDate referenceDate) {

		return reports
			.workRows(context.organizationId(), period.from(), period.to(), period.customerId(), period.page())
			.map(row -> toWork(row, referenceDate));
	}

	private ReportReadRepository.WorkTotals findTotals(Usecase.Context context, ReportPeriod period,
			LocalDate referenceDate) {

		return reports.workTotals(context.organizationId(), period.from(), period.to(), period.customerId(),
				referenceDate);
	}

	private WorkOrdersReport createReport(ReportPeriod period, ReportContext reportContext,
			ReportReadRepository.WorkTotals totals, PageResult<WorkOrdersReport.Work> works) {

		return new WorkOrdersReport(period.from(), period.to(), reportContext.referenceDate(), reportContext.timezone(),
				reportContext.currencyCode(), totals.realized(), totals.realizedPending(), totals.projection(), works);
	}

	private WorkOrdersReport.Work toWork(ReportReadRepository.WorkRow row, LocalDate referenceDate) {

		var realized = ReportRealization.isRealized(row.status(), row.serviceDate(), referenceDate);

		var pending = realized && !row.hasActivePayment();

		return new WorkOrdersReport.Work(row.workOrderId(), row.customerId(), row.customerName(), row.serviceDate(),
				row.status(), row.totalAmount(), realized, row.hasActivePayment(), pending);
	}

}
