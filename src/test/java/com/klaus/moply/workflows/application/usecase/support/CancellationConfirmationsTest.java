package com.klaus.moply.workflows.application.usecase.support;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workflows.application.usecase.dto.PaymentConfirmation;

import static org.junit.jupiter.api.Assertions.*;

class CancellationConfirmationsTest {

	@Test
	void shouldRejectInvalidConfirmationsBeforeReadingPayments() {
		for (var confirmation : java.util.Arrays.asList(null, new PaymentConfirmation(null, true, "reason"),
				new PaymentConfirmation(UUID.randomUUID(), false, "reason"),
				new PaymentConfirmation(UUID.randomUUID(), true, null),
				new PaymentConfirmation(UUID.randomUUID(), true, " "))) {
			assertThrows(DomainException.class,
					() -> CancellationConfirmations.from(java.util.Collections.singletonList(confirmation)));
		}
	}

	@Test
	void shouldCopyConfirmationsIntoImmutableMapAndRejectDuplicates() {
		var confirmation = new PaymentConfirmation(UUID.randomUUID(), true, "reason");
		var source = new java.util.ArrayList<>(List.of(confirmation));
		var confirmations = CancellationConfirmations.from(source);
		source.clear();
		assertEquals(confirmation, confirmations.byPayment().get(confirmation.paymentId()));
		assertThrows(UnsupportedOperationException.class, () -> confirmations.byPayment().clear());
		assertThrows(DomainException.class, () -> CancellationConfirmations.from(List.of(confirmation, confirmation)));
	}

}
