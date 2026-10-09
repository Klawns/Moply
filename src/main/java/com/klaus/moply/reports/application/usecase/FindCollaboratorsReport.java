package com.klaus.moply.reports.application.usecase;

import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.reports.application.ports.ReportReadRepository;
import com.klaus.moply.reports.application.usecase.dto.ReportContext;
import com.klaus.moply.reports.application.usecase.dto.CollaboratorsReport;
import com.klaus.moply.reports.application.usecase.dto.CollaboratorsReportInput;
import com.klaus.moply.reports.application.usecase.dto.ReportPeriod;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.exception.ApplicationException;

public class FindCollaboratorsReport implements Usecase.Contextual<CollaboratorsReportInput, CollaboratorsReport> {

	private final ReportReadRepository reports;

	private final ReportContextResolver contextResolver;

	public FindCollaboratorsReport(ReportReadRepository reports, ReportContextResolver contextResolver) {
		this.reports = reports;
		this.contextResolver = contextResolver;
	}

	@Override
	public CollaboratorsReport execute(Usecase.Context context, CollaboratorsReportInput input) {

		validateInput(input);

		var period = input.period();
		var collaboratorId = input.collaboratorId();

		var reportContext = contextResolver.resolve(context, period, collaboratorId);

		var assignments = findAssignments(context, period, collaboratorId, reportContext.referenceDate());

		var settlements = findSettlements(context, period, collaboratorId);

		var totals = findTotals(context, period, collaboratorId, reportContext.referenceDate());

		return createReport(period, reportContext, totals, assignments, settlements);
	}

	private void validateInput(CollaboratorsReportInput input) {
		if (input == null) {
			throw new ApplicationException("Informe o período.");
		}
	}

	private PageResult<CollaboratorsReport.Assignment> findAssignments(Usecase.Context context, ReportPeriod period,
			UUID collaboratorId, LocalDate referenceDate) {

		return reports
			.assignments(context.organizationId(), period.from(), period.to(), period.customerId(), collaboratorId,
					period.page())
			.map(row -> toAssignment(row, referenceDate));
	}

	private PageResult<CollaboratorsReport.Settlement> findSettlements(Usecase.Context context, ReportPeriod period,
			UUID collaboratorId) {

		return reports
			.settlements(context.organizationId(), period.from(), period.to(), period.customerId(), collaboratorId,
					period.page())
			.map(this::toSettlement);
	}

	private ReportReadRepository.CollaboratorTotals findTotals(Usecase.Context context, ReportPeriod period,
			UUID collaboratorId, LocalDate referenceDate) {

		return reports.collaboratorTotals(context.organizationId(), period.from(), period.to(), period.customerId(),
				collaboratorId, referenceDate);
	}

	private CollaboratorsReport createReport(ReportPeriod period, ReportContext reportContext,
			ReportReadRepository.CollaboratorTotals totals, PageResult<CollaboratorsReport.Assignment> assignments,
			PageResult<CollaboratorsReport.Settlement> settlements) {

		return new CollaboratorsReport(period.from(), period.to(), reportContext.referenceDate(),
				reportContext.timezone(), reportContext.currencyCode(), totals.allocated(), totals.realizedAllocated(),
				totals.futureAllocated(), totals.pending(), totals.realizedPending(), totals.futurePending(),
				totals.settlements(), assignments, settlements);
	}

	private CollaboratorsReport.Assignment toAssignment(ReportReadRepository.AssignmentRow row,
			LocalDate referenceDate) {

		var pendingAmount = row.allocatedAmount().subtract(row.activeSettlements());

		var realized = ReportRealization.isRealized(row.workStatus(), row.serviceDate(), referenceDate);

		return new CollaboratorsReport.Assignment(row.workOrderId(), row.customerId(), row.customerName(),
				row.serviceDate(), row.workStatus(), row.collaboratorId(), row.collaboratorName(),
				row.allocatedAmount(), row.activeSettlements(), pendingAmount, realized);
	}

	private CollaboratorsReport.Settlement toSettlement(ReportReadRepository.SettlementRow row) {

		return new CollaboratorsReport.Settlement(row.paymentId(), row.workOrderId(), row.customerId(),
				row.customerName(), row.serviceDate(), row.collaboratorId(), row.collaboratorName(), row.paidOn(),
				row.amount());
	}

}
