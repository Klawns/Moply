package com.klaus.moply.payments.infra.web.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record RecordPaymentRequest(@NotNull LocalDate paidOn) {
}
