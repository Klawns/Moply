package com.klaus.moply.payments.application.usecase.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {

	public PaymentNotFoundException(UUID id) {
		super("Pagamento não encontrado: " + id);
	}

}
