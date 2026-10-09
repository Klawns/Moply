package com.klaus.moply.payments.application.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.klaus.moply.payments.application.usecase.dto.ListWorkOrderPaymentsInput;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.application.usecase.exception.ApplicationException;

class ListWorkOrderPaymentsTest {

	@Test
	void shouldRejectMissingWorkOrderInput() {
		var usecase = new ListWorkOrderPayments(null, null);

		assertThrows(ApplicationException.class, () -> usecase.execute(new Context(UUID.randomUUID()), null));
		assertThrows(ApplicationException.class,
				() -> usecase.execute(new Context(UUID.randomUUID()), new ListWorkOrderPaymentsInput(null)));
	}

}
