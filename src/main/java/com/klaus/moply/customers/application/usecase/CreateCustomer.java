package com.klaus.moply.customers.application.usecase;

import java.util.UUID;
import com.klaus.moply.application.usecase.Usecase;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CreateCustomerInput;
import com.klaus.moply.customers.domain.Customer;
import com.klaus.moply.customers.domain.CustomerLocation;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateCustomer implements Usecase<CreateCustomerInput, UUID> {

	private final CustomerRepository repo;

	@Override
	public UUID execute(CreateCustomerInput input) {
		Customer customer = Customer.create(input.name(), input.phone(), input.email(), input.notes(),
				input.locations()
					.stream()
					.map(location -> CustomerLocation.create(location.name(), location.address(), location.notes()))
					.toList());
		return repo.save(customer).getId();
	}

}
