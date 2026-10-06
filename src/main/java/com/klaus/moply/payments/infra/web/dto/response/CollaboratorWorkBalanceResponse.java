package com.klaus.moply.payments.infra.web.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.payments.application.usecase.CollaboratorPaymentSummary;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

public record CollaboratorWorkBalanceResponse(UUID workOrderId, LocalDate serviceDate, WorkOrderStatus status,
		PaymentBalanceResponse balance) {

	public static CollaboratorWorkBalanceResponse from(CollaboratorPaymentSummary.WorkBalance b) {
		return new CollaboratorWorkBalanceResponse(b.workOrderId(), b.serviceDate(), b.status(),
				new PaymentBalanceResponse(b.allocatedAmount(), b.recordedAmount(), b.remainingAmount(),
						b.requiresAttention()));
	}

}
