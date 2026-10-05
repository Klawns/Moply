package com.klaus.moply.customers.application.usecase;

import java.util.UUID;

import com.klaus.moply.customers.application.ports.CustomerRepository;
import com.klaus.moply.customers.application.usecase.dto.CustomerLocationOutput;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindCustomerLocations
		implements Usecase.Contextual<FindCustomerLocations.Filter, PageResult<CustomerLocationOutput>> {

	private final CustomerRepository repo;

	@Override
	public PageResult<CustomerLocationOutput> execute(Context context, Filter input) {
		return repo.findLocations(context.organizationId(), input.customerId(), input.page())
			.map(CustomerLocationOutput::fromDomain);
	}

	public record Filter(UUID customerId, PageQuery page) {
	}

}
