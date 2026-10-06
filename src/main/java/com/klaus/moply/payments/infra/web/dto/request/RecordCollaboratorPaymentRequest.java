package com.klaus.moply.payments.infra.web.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record RecordCollaboratorPaymentRequest(@NotNull @DecimalMin("0.01") BigDecimal amount,
		@NotNull LocalDate paidOn) {
}
