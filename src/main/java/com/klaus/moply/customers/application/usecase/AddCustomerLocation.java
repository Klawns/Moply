package com.klaus.moply.customers.application.usecase;

import com.klaus.moply.customers.application.usecase.dto.AddCustomerLocationInput;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CustomerLocationOutput;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.customers.domain.entities.CustomerLocation;
import com.klaus.moply.shared.application.usecase.Usecase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AddCustomerLocation implements Usecase<AddCustomerLocationInput, CustomerLocationOutput> {

	private final CustomerRepository repo;

	@Override
	public CustomerLocationOutput execute(AddCustomerLocationInput input) {
		Customer customer = repo.findById(input.customerId())
			.orElseThrow(() -> new CustomerNotFoundException(input.customerId()));
		CustomerLocation location = CustomerLocation.create(input.name(), input.address(), input.notes());
		Customer saved = repo.save(customer.addLocation(location));
		return CustomerLocationOutput.fromDomain(saved.findLocation(location.getId()));
	}

}
