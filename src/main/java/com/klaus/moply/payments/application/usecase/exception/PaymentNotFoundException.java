package com.klaus.moply.payments.application.usecase.exception;

import java.util.UUID;

import com.klaus.moply.shared.application.usecase.exception.ApplicationException;

public class PaymentNotFoundException extends ApplicationException {

	public PaymentNotFoundException(UUID id) {
		super("PAYMENT_NOT_FOUND", "Pagamento não encontrado: " + id);
	}

}
