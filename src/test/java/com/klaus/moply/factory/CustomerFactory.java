package com.klaus.moply.factory;

import java.util.List;
import java.util.UUID;

import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.customers.domain.entities.CustomerLocation;

public class CustomerFactory {

	public static Customer restoreCustomer(UUID id) {
		return Customer.restore(
				id,
				"  Maria Silva  ");
	}

	public static Customer restoreCustomerWithContacts(UUID id) {
		return Customer.restore(
				id,
				"Maria",
				"+55 11 99999-1234",
				"Maria@example.com",
				"Contato à tarde");
	}

	public static Customer restoreCustomerWithLocations(UUID id) {
		var locations = List.of(
				CustomerLocation.create(
						"Casa",
						"Rua A, 123",
						"Portão azul"));

		return Customer.restore(
				id,
				"Maria",
				"+55 11 99999-1234",
				"Maria@example.com",
				"Contato à tarde",
				locations);
	}
}