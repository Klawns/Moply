package com.klaus.moply.reports.infra.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.klaus.moply.reports.application.ports.ReportReadRepository;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.infra.persistence.PageableMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportReadJpaAdapter implements ReportReadRepository {

	private static final String SERVICE_DATE = "serviceDate";

	private static final String PAID_ON = "paidOn";

	private static final Set<String> WORK_SORT_FIELDS = Set.of(SERVICE_DATE, "status", "id");

	private static final Set<String> PAYMENT_SORT_FIELDS = Set.of(PAID_ON, "amount", "id");

	private static final Set<String> ASSIGNMENT_SORT_FIELDS = Set.of("collaboratorName", SERVICE_DATE, "id");

	private final ReportJpaRepository repository;

	@Override
	public PageResult<WorkRow> workRows(UUID account, LocalDate from, LocalDate to, UUID customerId, PageQuery page) {
		var pageable = PageableMapper.toPageable(page, WORK_SORT_FIELDS, SERVICE_DATE);
		return PageableMapper.toResult(repository.workRows(account, from, to, customerId, pageable),
				row -> new WorkRow(row.getId(), row.getCustomerId(), row.getCustomerName(), row.getServiceDate(),
						row.getStatus(), row.getCurrencyCode(), row.getTotalAmount(), row.getHasActivePayment()));
	}

	@Override
	public WorkTotals workTotals(UUID account, LocalDate from, LocalDate to, UUID customerId, LocalDate today) {
		var row = repository.workTotals(account, from, to, customerId, today);
		return new WorkTotals(row.getRealized(), row.getRealizedPending(), row.getProjection());
	}

	@Override
	public PageResult<PaymentRow> customerPayments(UUID account, LocalDate from, LocalDate to, UUID customerId,
			PageQuery page) {
		var pageable = PageableMapper.toPageable(page, PAYMENT_SORT_FIELDS, PAID_ON);
		return PageableMapper.toResult(repository.customerPayments(account, from, to, customerId, pageable),
				row -> new PaymentRow(row.getId(), row.getWorkOrderId(), row.getCustomerId(), row.getCustomerName(),
						row.getPaidOn(), row.getCurrencyCode(), row.getAmount()));
	}

	@Override
	public BigDecimal customerPaymentTotal(UUID account, LocalDate from, LocalDate to, UUID customerId) {
		return repository.customerPaymentTotal(account, from, to, customerId);
	}

	@Override
	public PageResult<AssignmentRow> assignments(UUID account, LocalDate from, LocalDate to, UUID customerId,
			UUID collaboratorId, PageQuery page) {
		var pageable = PageableMapper.toPageable(page, ASSIGNMENT_SORT_FIELDS, "collaboratorName");
		return PageableMapper.toResult(repository.assignments(account, from, to, customerId, collaboratorId, pageable),
				row -> new AssignmentRow(row.getWorkOrderId(), row.getCustomerId(), row.getCustomerName(),
						row.getServiceDate(), row.getWorkStatus(), row.getCurrencyCode(), row.getCollaboratorId(),
						row.getCollaboratorName(), row.getAllocatedAmount(), row.getActiveSettlements()));
	}

	@Override
	public CollaboratorTotals collaboratorTotals(UUID account, LocalDate from, LocalDate to, UUID customerId,
			UUID collaboratorId, LocalDate today) {
		var row = repository.collaboratorTotals(account, from, to, customerId, collaboratorId, today);
		var settlements = repository.settlementTotal(account, from, to, customerId, collaboratorId);
		return new CollaboratorTotals(row.getAllocated(), row.getRealizedAllocated(), row.getFutureAllocated(),
				row.getPending(), row.getRealizedPending(), row.getFuturePending(), settlements);
	}

	@Override
	public PageResult<SettlementRow> settlements(UUID account, LocalDate from, LocalDate to, UUID customerId,
			UUID collaboratorId, PageQuery page) {
		var pageable = PageableMapper.toPageable(page, PAYMENT_SORT_FIELDS, PAID_ON);
		return PageableMapper.toResult(repository.settlements(account, from, to, customerId, collaboratorId, pageable),
				row -> new SettlementRow(row.getId(), row.getWorkOrderId(), row.getCustomerId(), row.getCustomerName(),
						row.getServiceDate(), row.getWorkStatus(), row.getCollaboratorId(), row.getCollaboratorName(),
						row.getPaidOn(), row.getCurrencyCode(), row.getAmount()));
	}

}
