package com.klaus.moply.factory;

import java.util.UUID;

import com.klaus.moply.customers.domain.entities.Customer;

public class CustomerFactory {

	public static Customer restoreCustomer(UUID id) {
		return Customer.restore(id, "  Maria Silva  ");
	}

	public static Customer restoreCustomerWithContacts(UUID id) {
		return Customer.restore(id, "Maria", "+55 11 99999-1234", "Maria@example.com", "Contato à tarde");
	}

}
