package com.klaus.moply.customers.application.usecase;

import com.klaus.moply.customers.application.ports.LocationReadRepository;
import com.klaus.moply.customers.application.usecase.dto.LocationSummary;
import com.klaus.moply.shared.application.pagination.PageQuery;
import com.klaus.moply.shared.application.pagination.PageResult;
import com.klaus.moply.shared.application.usecase.Usecase;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FindAllLocations implements Usecase.Contextual<FindAllLocations.Filter, PageResult<LocationSummary>> {

	private final LocationReadRepository repository;

	@Override
	public PageResult<LocationSummary> execute(Context context, Filter input) {
		return repository.findAll(context.organizationId(), input.query() == null ? "" : input.query().trim(),
				input.page());
	}

	public record Filter(String query, PageQuery page) {
	}

}
