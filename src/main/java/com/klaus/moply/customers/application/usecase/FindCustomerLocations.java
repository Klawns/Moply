package com.klaus.moply.customers.application.usecase;

import java.util.UUID;
import java.util.List;
import com.klaus.moply.orderservice.application.usecase.Usecase;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CustomerLocationOutput;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindCustomerLocations implements Usecase<UUID, List<CustomerLocationOutput>> {

	private final CustomerRepository repo;

	@Override
	public List<CustomerLocationOutput> execute(UUID input) {
		Customer customer = repo.findById(input).orElseThrow(() -> new CustomerNotFoundException(input));
		return customer.getLocations().stream().map(CustomerLocationOutput::fromDomain).toList();
	}

}
