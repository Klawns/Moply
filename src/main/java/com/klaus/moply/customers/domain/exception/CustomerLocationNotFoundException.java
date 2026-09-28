package com.klaus.moply.customers.domain.exception;

import java.util.UUID;
import com.klaus.moply.orderservice.domain.exception.DomainException;

public class CustomerLocationNotFoundException extends DomainException {

	public CustomerLocationNotFoundException(UUID id) {
		super("Local não encontrado no cliente: " + id);
	}

}
