package com.klaus.moply.customers.application.usecase;

import com.klaus.moply.customers.application.usecase.dto.AddCustomerLocationInput;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CustomerLocationOutput;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.customers.domain.entities.CustomerLocation;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AddCustomerLocation implements Usecase.Contextual<AddCustomerLocationInput, CustomerLocationOutput> {

	private final CustomerRepository repo;

	@Override
	public CustomerLocationOutput execute(Context context, AddCustomerLocationInput input) {
		Customer customer = repo.findById(context.organizationId(), input.customerId())
			.orElseThrow(() -> new CustomerNotFoundException(input.customerId()));

		CustomerLocation location = CustomerLocation.create(input.name(), input.address(), input.notes());
		Customer saved = repo.save(context.organizationId(), customer.addLocation(location));
		return CustomerLocationOutput.fromDomain(saved.findLocation(location.getId()));
	}

}
