package com.klaus.moply.payments.infra.web.dto.response;

import java.util.List;
import java.util.UUID;

import com.klaus.moply.payments.application.usecase.CollaboratorPaymentSummary;

public record CollaboratorPaymentSummaryResponse(UUID collaboratorId, String currencyCode,
		PaymentBalanceResponse balance, List<CollaboratorWorkBalanceResponse> workOrders) {

	public static CollaboratorPaymentSummaryResponse from(CollaboratorPaymentSummary s) {
		return new CollaboratorPaymentSummaryResponse(s.collaboratorId(), s.currencyCode(),
				new PaymentBalanceResponse(s.allocatedAmount(), s.recordedAmount(), s.remainingAmount(),
						s.requiresAttention()),
				s.workOrders().stream().map(CollaboratorWorkBalanceResponse::from).toList());
	}

}
