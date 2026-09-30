package com.klaus.moply.customers.application.usecase;

import java.util.UUID;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CustomerOutput;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindCustomerById implements Usecase.Contextual<UUID, CustomerOutput> {

	private final CustomerRepository repo;

	@Override
	public CustomerOutput execute(Context context, UUID input) {
		var id = input;
		Customer customer = repo.findById(context.organizationId(), id)
			.orElseThrow(() -> new CustomerNotFoundException(id));
		return CustomerOutput.fromDomain(customer);
	}

}
