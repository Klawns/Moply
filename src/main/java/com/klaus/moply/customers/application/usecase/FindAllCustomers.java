package com.klaus.moply.customers.application.usecase;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CustomerOutput;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindAllCustomers implements Usecase.Contextual<PageQuery, PageResult<CustomerOutput>> {

	private final CustomerRepository repo;

	@Override
	public PageResult<CustomerOutput> execute(Context context, PageQuery input) {
		return repo.findAll(context.organizationId(), input).map(CustomerOutput::fromDomain);
	}

}
