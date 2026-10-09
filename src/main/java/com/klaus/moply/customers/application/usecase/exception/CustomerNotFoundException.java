package com.klaus.moply.customers.application.usecase.exception;

import java.util.UUID;

import com.klaus.moply.shared.application.usecase.exception.ApplicationException;

public class CustomerNotFoundException extends ApplicationException {

	public CustomerNotFoundException(UUID id) {
		super("CUSTOMER_NOT_FOUND", "Cliente não encontrado: " + id);
	}

}
