package com.klaus.moply.payments.application.usecase.dto;

import java.util.UUID;

public record ReverseCollaboratorPaymentInput(UUID workOrderId, UUID collaboratorId, UUID paymentId,
		boolean confirmNotActuallyPaid, String reason, UUID actorId) {
}
