package com.klaus.moply.payments.application.usecase.exception;

public class PaymentConflictException extends RuntimeException {

	public PaymentConflictException(String message) {
		super(message);
	}

}
