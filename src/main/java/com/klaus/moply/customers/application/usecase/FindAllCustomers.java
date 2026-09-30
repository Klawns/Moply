package com.klaus.moply.customers.application.usecase;

import java.util.List;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CustomerOutput;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindAllCustomers implements Usecase.Contextual<Void, List<CustomerOutput>> {

	private final CustomerRepository repo;

	@Override
	public List<CustomerOutput> execute(Context context, Void input) {
		return repo.findAll(context.organizationId()).stream().map(CustomerOutput::fromDomain).toList();
	}

}
