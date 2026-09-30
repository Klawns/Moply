package com.klaus.moply.customers.application.usecase;

import java.util.UUID;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CreateCustomerInput;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.customers.domain.entities.CustomerLocation;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateCustomer implements Usecase.Contextual<CreateCustomerInput, UUID> {

	private final CustomerRepository repo;

	@Override
	public UUID execute(Context context, CreateCustomerInput input) {
		Customer customer = Customer.create(input.name(), input.phone(), input.email(), input.notes(),
				input.locations()
					.stream()
					.map(location -> CustomerLocation.create(location.name(), location.address(), location.notes()))
					.toList());
		return repo.save(context.organizationId(), customer).getId();
	}

}
