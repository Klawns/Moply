package com.klaus.moply.payments.application.usecase.dto;

import java.time.LocalDate;
import java.util.UUID;

public record RecordWorkOrderPaymentInput(UUID workOrderId, LocalDate paidOn, UUID actorId) {
}
