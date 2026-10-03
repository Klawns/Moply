package com.klaus.moply.payments.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.shared.domain.exception.DomainException;

class PaymentTest {

	private final UUID organizationId = UUID.randomUUID();

	private final UUID actorId = UUID.randomUUID();

	private final PaymentAmount amount = new PaymentAmount(new BigDecimal("120.00"), "GBP");

	private final LocalDate paidOn = LocalDate.parse("2026-10-01");

	private final Instant recordedAt = Instant.parse("2026-10-01T12:00:00Z");

	@Test
	void shouldCreateRecordedPaymentWithoutResourceSpecificAssociation() {
		var payment = Payment.create(organizationId, amount, paidOn, recordedAt, actorId);

		assertFalse(Payment.class.isRecord());
		assertNotNull(payment.id());
		assertEquals(organizationId, payment.organizationId());
		assertEquals(amount, payment.amount());
		assertEquals(paidOn, payment.paidOn());
		assertEquals(recordedAt, payment.recordedAt());
		assertEquals(actorId, payment.recordedBy());
		assertEquals(Payment.Status.RECORDED, payment.status());
	}

	@Test
	void shouldRestoreRecordedAndReversedPayments() {
		var id = UUID.randomUUID();
		var reversal = new PaymentReversal(recordedAt.plusSeconds(60), actorId, "Duplicate entry");

		var recorded = Payment.restore(id, organizationId, amount, paidOn, recordedAt, actorId, Payment.Status.RECORDED,
				null);
		var reversed = Payment.restore(id, organizationId, amount, paidOn, recordedAt, actorId, Payment.Status.REVERSED,
				reversal);

		assertEquals(Payment.Status.RECORDED, recorded.status());
		assertEquals(Payment.Status.REVERSED, reversed.status());
		assertEquals(reversal, reversed.reversal());
	}

	@Test
	void shouldPreserveOriginalPaymentDataWhenReversed() {
		var payment = Payment.create(organizationId, amount, paidOn, recordedAt, actorId);
		var reversed = payment.reverse(UUID.randomUUID(), "  Duplicate entry  ", recordedAt.plusSeconds(60));

		assertEquals(payment.id(), reversed.id());
		assertEquals(payment.organizationId(), reversed.organizationId());
		assertEquals(payment.amount(), reversed.amount());
		assertEquals(payment.paidOn(), reversed.paidOn());
		assertEquals(payment.recordedAt(), reversed.recordedAt());
		assertEquals(payment.recordedBy(), reversed.recordedBy());
		assertEquals("Duplicate entry", reversed.reversal().reason());
		assertEquals(Payment.Status.REVERSED, reversed.status());
	}

	@Test
	void shouldRejectInconsistentRestoredStateAndRepeatedReversal() {
		var id = UUID.randomUUID();
		assertThrows(DomainException.class, () -> Payment.restore(id, organizationId, amount, paidOn, recordedAt,
				actorId, Payment.Status.RECORDED, new PaymentReversal(recordedAt, actorId, "Mistake")));

		var reversed = Payment.create(organizationId, amount, paidOn, recordedAt, actorId)
			.reverse(actorId, "Mistake", recordedAt.plusSeconds(60));
		assertThrows(DomainException.class, () -> reversed.reverse(actorId, "Again", recordedAt.plusSeconds(120)));
	}

}
