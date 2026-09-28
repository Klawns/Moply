package com.klaus.moply.customers.application.usecase;

import java.util.List;
import com.klaus.moply.application.usecase.Usecase;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CustomerOutput;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindAllCustomers implements Usecase<Void, List<CustomerOutput>> {

	private final CustomerRepository repo;

	@Override
	public List<CustomerOutput> execute(Void input) {
		return repo.findAll().stream().map(CustomerOutput::fromDomain).toList();
	}

}
