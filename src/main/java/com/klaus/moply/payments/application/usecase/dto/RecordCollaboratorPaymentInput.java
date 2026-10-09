package com.klaus.moply.payments.application.usecase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecordCollaboratorPaymentInput(UUID workOrderId, UUID collaboratorId, BigDecimal amount, LocalDate paidOn,
		String idempotencyKey, UUID actorId) {
}
