package com.klaus.moply.customers.application.usecase;

import com.klaus.moply.orderservice.application.usecase.Usecase;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CustomerOutput;
import com.klaus.moply.customers.application.usecase.dto.UpdateCustomerInput;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateCustomer implements Usecase<UpdateCustomerInput, CustomerOutput> {

	private final CustomerRepository repo;

	@Override
	public CustomerOutput execute(UpdateCustomerInput input) {
		Customer customer = repo.findById(input.id()).orElseThrow(() -> new CustomerNotFoundException(input.id()));
		return CustomerOutput
			.fromDomain(repo.save(customer.update(input.name(), input.phone(), input.email(), input.notes())));
	}

}
