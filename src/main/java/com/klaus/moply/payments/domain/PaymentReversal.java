package com.klaus.moply.payments.domain;

import java.time.Instant;
import java.util.UUID;

import com.klaus.moply.shared.domain.exception.DomainException;

public record PaymentReversal(Instant at, UUID by, String reason) {

	public PaymentReversal {
		if (at == null || by == null || reason == null || reason.isBlank())
			throw new DomainException("Reversão exige instante, responsável e motivo.");
		reason = reason.strip();
	}

}
