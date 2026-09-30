package com.klaus.moply.customers.infra.web.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.auth.infra.security.AccountPrincipal;
import com.klaus.moply.customers.application.usecase.AddCustomerLocation;
import com.klaus.moply.customers.application.usecase.FindCustomerLocationById;
import com.klaus.moply.customers.application.usecase.FindCustomerLocations;
import com.klaus.moply.customers.application.usecase.UpdateCustomerLocation;
import com.klaus.moply.customers.application.usecase.dto.AddCustomerLocationInput;
import com.klaus.moply.customers.application.usecase.dto.FindCustomerLocationByIdInput;
import com.klaus.moply.customers.application.usecase.dto.UpdateCustomerLocationInput;
import com.klaus.moply.customers.infra.web.dto.CustomerLocationRequest;
import com.klaus.moply.customers.infra.web.dto.CustomerLocationResponse;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/locations")
@RequiredArgsConstructor
public class CustomerLocationController {

	private final AddCustomerLocation addLocation;

	private final FindCustomerLocations findLocations;

	private final FindCustomerLocationById findLocation;

	private final UpdateCustomerLocation updateLocation;

	@PostMapping
	public ResponseEntity<CustomerLocationResponse> addLocation(@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID customerId, @Valid @RequestBody CustomerLocationRequest request) {
		var output = addLocation.execute(new Context(principal.getOrganizationId()),
				new AddCustomerLocationInput(customerId, request.name(), request.address(), request.notes()));
		return ResponseEntity.created(URI.create("/api/v1/customers/" + customerId + "/locations/" + output.id()))
			.body(CustomerLocationResponse.from(output));
	}

	@GetMapping
	public List<CustomerLocationResponse> locations(@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID customerId) {
		return findLocations.execute(new Context(principal.getOrganizationId()), customerId)
			.stream()
			.map(CustomerLocationResponse::from)
			.toList();
	}

	@GetMapping("/{locationId}")
	public CustomerLocationResponse location(@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID customerId, @PathVariable UUID locationId) {
		return CustomerLocationResponse.from(findLocation.execute(new Context(principal.getOrganizationId()),
				new FindCustomerLocationByIdInput(customerId, locationId)));
	}

	@PutMapping("/{locationId}")
	public CustomerLocationResponse updateLocation(@AuthenticationPrincipal AccountPrincipal principal,
			@PathVariable UUID customerId, @PathVariable UUID locationId,
			@Valid @RequestBody CustomerLocationRequest request) {
		return CustomerLocationResponse
			.from(updateLocation.execute(new Context(principal.getOrganizationId()), new UpdateCustomerLocationInput(
					customerId, locationId, request.name(), request.address(), request.notes())));
	}

}
