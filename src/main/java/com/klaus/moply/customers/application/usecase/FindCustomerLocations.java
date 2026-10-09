package com.klaus.moply.customers.application.usecase;

import com.klaus.moply.customers.application.usecase.dto.FindCustomerLocationsFilter;
import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CustomerLocationOutput;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindCustomerLocations
		implements Usecase.Contextual<FindCustomerLocationsFilter, PageResult<CustomerLocationOutput>> {

	private final CustomerRepository repo;

	@Override
	public PageResult<CustomerLocationOutput> execute(Context context, FindCustomerLocationsFilter input) {
		return repo.findLocations(context.organizationId(), input.customerId(), input.page())
			.map(CustomerLocationOutput::fromDomain);
	}

}
