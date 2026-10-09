package com.klaus.moply.customers.infra.web.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.klaus.moply.customers.application.usecase.dto.FindAllLocationsFilter;
import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.customers.application.usecase.FindAllLocations;
import com.klaus.moply.customers.infra.web.api.LocationApi;
import com.klaus.moply.customers.infra.web.dto.LocationSummaryResponse;
import com.klaus.moply.shared.application.usecase.Usecase.Context;
import com.klaus.moply.shared.infra.web.PageQueryRequest;
import com.klaus.moply.shared.infra.web.dto.PageResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController implements LocationApi {

	private final FindAllLocations findLocations;

	@GetMapping(produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
	@Override
	public PageResponse<LocationSummaryResponse> list(@AuthenticationPrincipal AccountPrincipal principal,
			@RequestParam(required = false) String q, @RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size, @RequestParam(required = false) String sort,
			@RequestParam(required = false) String direction) {
		return PageResponse.from(
				findLocations.execute(new Context(principal.getOrganizationId()),
						new FindAllLocationsFilter(q, PageQueryRequest.toQuery(page, size, sort, direction))),
				LocationSummaryResponse::from);
	}

}
