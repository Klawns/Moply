package com.klaus.moply.customers.application.usecase;

import com.klaus.moply.customers.application.usecase.dto.UpdateCustomerLocationInput;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CustomerLocationOutput;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateCustomerLocation implements Usecase.Contextual<UpdateCustomerLocationInput, CustomerLocationOutput> {

	private final CustomerRepository repo;

	@Override
	public CustomerLocationOutput execute(Context context, UpdateCustomerLocationInput input) {
		Customer customer = repo.findById(context.organizationId(), input.customerId())
			.orElseThrow(() -> new CustomerNotFoundException(input.customerId()));
		Customer updated = customer.updateLocation(input.locationId(), input.name(), input.address(), input.notes());
		return CustomerLocationOutput
			.fromDomain(repo.save(context.organizationId(), updated).findLocation(input.locationId()));
	}

}
