package com.klaus.moply.customers.application.usecase;

import com.klaus.moply.customers.application.usecase.dto.FindCustomerLocationByIdInput;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CustomerLocationOutput;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindCustomerLocationById
		implements Usecase.Contextual<FindCustomerLocationByIdInput, CustomerLocationOutput> {

	private final CustomerRepository repo;

	@Override
	public CustomerLocationOutput execute(Context context, FindCustomerLocationByIdInput input) {
		Customer customer = repo.findById(context.organizationId(), input.customerId())
			.orElseThrow(() -> new CustomerNotFoundException(input.customerId()));
		return CustomerLocationOutput.fromDomain(customer.findLocation(input.locationId()));
	}

}
