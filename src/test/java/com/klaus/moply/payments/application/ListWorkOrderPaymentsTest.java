package com.klaus.moply.payments.application;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.payments.application.usecase.ListWorkOrderPayments;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.domain.exception.DomainException;

class ListWorkOrderPaymentsTest {

	@Test
	void shouldRejectMissingWorkOrderInput() {
		var usecase = new ListWorkOrderPayments(null, null);

		assertThrows(DomainException.class, () -> usecase.execute(new Context(UUID.randomUUID()), null));
		assertThrows(DomainException.class,
				() -> usecase.execute(new Context(UUID.randomUUID()), new ListWorkOrderPayments.Input(null)));
	}

}
