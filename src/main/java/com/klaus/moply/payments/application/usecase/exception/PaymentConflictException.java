package com.klaus.moply.payments.application.usecase.exception;

import com.klaus.moply.shared.application.usecase.exception.ApplicationException;

public class PaymentConflictException extends ApplicationException {

	public PaymentConflictException(String message) {
		super("PAYMENT_CONFLICT", message);
	}

}
