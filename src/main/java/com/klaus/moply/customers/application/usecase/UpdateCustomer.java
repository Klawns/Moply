package com.klaus.moply.customers.application.usecase;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CustomerOutput;
import com.klaus.moply.customers.application.usecase.dto.UpdateCustomerInput;
import com.klaus.moply.customers.application.usecase.exception.CustomerNotFoundException;
import com.klaus.moply.customers.domain.entities.Customer;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateCustomer implements Usecase.Contextual<UpdateCustomerInput, CustomerOutput> {

	private final CustomerRepository repo;

	@Override
	public CustomerOutput execute(Context context, UpdateCustomerInput input) {
		Customer customer = repo.findById(context.organizationId(), input.id())
			.orElseThrow(() -> new CustomerNotFoundException(input.id()));
		return CustomerOutput.fromDomain(repo.save(context.organizationId(),
				customer.update(input.name(), input.phone(), input.email(), input.notes())));
	}

}
