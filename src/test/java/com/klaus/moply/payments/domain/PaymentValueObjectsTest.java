package com.klaus.moply.payments.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.shared.domain.exception.DomainException;

class PaymentValueObjectsTest {

	@Test
	void shouldNormalizeIntegralAmountAndRetainItsCurrency() {
		var amount = new PaymentAmount(new BigDecimal("34.5"), "GBP");
		assertEquals(new BigDecimal("34.50"), amount.value());
		assertEquals("GBP", amount.currencyCode());
		assertThrows(DomainException.class, () -> new PaymentAmount(BigDecimal.ZERO, "GBP"));
		assertThrows(DomainException.class, () -> new PaymentAmount(BigDecimal.ONE, "USD"));
	}

	@Test
	void shouldRequireCompleteReversalAuditAndNormalizeReason() {
		var actor = UUID.randomUUID();
		var at = Instant.parse("2026-09-30T12:00:00Z");
		assertEquals("Mistake", new PaymentReversal(at, actor, "  Mistake  ").reason());
		assertThrows(DomainException.class, () -> new PaymentReversal(at, actor, "  "));
	}

}
