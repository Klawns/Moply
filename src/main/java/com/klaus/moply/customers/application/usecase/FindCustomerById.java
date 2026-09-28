package com.klaus.moply.customers.application.usecase;

import java.util.UUID;
import com.klaus.moply.orderservice.application.usecase.Usecase;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CustomerOutput;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindCustomerById implements Usecase<UUID, CustomerOutput> {

	private final CustomerRepository repo;

	@Override
	public CustomerOutput execute(UUID id) {
		Customer customer = repo.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
		return CustomerOutput.fromDomain(customer);
	}

}
