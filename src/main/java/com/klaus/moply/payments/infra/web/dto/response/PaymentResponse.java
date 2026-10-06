package com.klaus.moply.payments.infra.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.klaus.moply.payments.domain.Payment;

public record PaymentResponse(UUID id, BigDecimal amount, String currencyCode, LocalDate paidOn, Payment.Status status,
		PaymentRecordingResponse recording, PaymentReversalResponse reversal) {

	public static PaymentResponse from(Payment p) {
		var r = p.reversal();
		return new PaymentResponse(p.id(), p.amount().value(), p.amount().currencyCode(), p.paidOn(), p.status(),
				new PaymentRecordingResponse(p.recordedAt(), p.recordedBy()),
				r == null ? null : new PaymentReversalResponse(r.at(), r.by(), r.reason()));
	}

}
