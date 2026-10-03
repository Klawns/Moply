package com.klaus.moply.payments.infra.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.payments.domain.Payment;

public record PaymentResponse(UUID id, BigDecimal amount, String currencyCode, LocalDate paidOn, Instant recordedAt,
		UUID recordedBy, Payment.Status status, Instant reversedAt, UUID reversedBy, String reversalReason) {

	public static PaymentResponse from(Payment payment) {
		var reversal = payment.reversal();
		return new PaymentResponse(payment.id(), payment.amount().value(), payment.amount().currencyCode(),
				payment.paidOn(), payment.recordedAt(), payment.recordedBy(), payment.status(),
				reversal == null ? null : reversal.at(), reversal == null ? null : reversal.by(),
				reversal == null ? null : reversal.reason());
	}

}
