package com.klaus.moply.customers.application.usecase;

import com.klaus.moply.customers.application.usecase.dto.FindCustomerLocationByIdInput;
import com.klaus.moply.orderservice.application.usecase.Usecase;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CustomerLocationOutput;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindCustomerLocationById implements Usecase<FindCustomerLocationByIdInput, CustomerLocationOutput> {

	private final CustomerRepository repo;

	@Override
	public CustomerLocationOutput execute(FindCustomerLocationByIdInput input) {
		Customer customer = repo.findById(input.customerId())
			.orElseThrow(() -> new CustomerNotFoundException(input.customerId()));
		return CustomerLocationOutput.fromDomain(customer.findLocation(input.locationId()));
	}

}
