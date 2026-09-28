package com.klaus.moply.customers.application.usecase.exception;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {

	public CustomerNotFoundException(UUID id) {
		super("Cliente não encontrado: " + id);
	}

}
