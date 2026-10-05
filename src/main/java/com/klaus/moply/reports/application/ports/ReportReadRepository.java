package com.klaus.moply.reports.application.ports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;

public interface ReportReadRepository {

	PageResult<WorkRow> workRows(UUID account, LocalDate from, LocalDate to, UUID customerId, PageQuery page);

	WorkTotals workTotals(UUID account, LocalDate from, LocalDate to, UUID customerId, LocalDate today);

	PageResult<PaymentRow> customerPayments(UUID account, LocalDate from, LocalDate to, UUID customerId,
			PageQuery page);

	BigDecimal customerPaymentTotal(UUID account, LocalDate from, LocalDate to, UUID customerId);

	PageResult<AssignmentRow> assignments(UUID account, LocalDate from, LocalDate to, UUID customerId,
			UUID collaboratorId, PageQuery page);

	CollaboratorTotals collaboratorTotals(UUID account, LocalDate from, LocalDate to, UUID customerId,
			UUID collaboratorId, LocalDate today);

	PageResult<SettlementRow> settlements(UUID account, LocalDate from, LocalDate to, UUID customerId,
			UUID collaboratorId, PageQuery page);

	record WorkTotals(BigDecimal realized, BigDecimal realizedPending, BigDecimal projection) {
	}

	record CollaboratorTotals(BigDecimal allocated, BigDecimal realizedAllocated, BigDecimal futureAllocated,
			BigDecimal pending, BigDecimal realizedPending, BigDecimal futurePending, BigDecimal settlements) {
	}

	record WorkRow(UUID workOrderId, UUID customerId, String customerName, LocalDate serviceDate, String status,
			String currencyCode, BigDecimal totalAmount, boolean hasActivePayment) {
	}

	record PaymentRow(UUID paymentId, UUID workOrderId, UUID customerId, String customerName, LocalDate paidOn,
			String currencyCode, BigDecimal amount) {
	}

	record AssignmentRow(UUID workOrderId, UUID customerId, String customerName, LocalDate serviceDate,
			String workStatus, String currencyCode, UUID collaboratorId, String collaboratorName,
			BigDecimal allocatedAmount, BigDecimal activeSettlements) {
	}

	record SettlementRow(UUID paymentId, UUID workOrderId, UUID customerId, String customerName, LocalDate serviceDate,
			String workStatus, UUID collaboratorId, String collaboratorName, LocalDate paidOn, String currencyCode,
			BigDecimal amount) {
	}

}
