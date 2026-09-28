package com.klaus.moply.customers.infra.web.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klaus.moply.customers.application.usecase.AddCustomerLocation;
import com.klaus.moply.customers.application.usecase.CreateCustomer;
import com.klaus.moply.customers.application.usecase.FindAllCustomers;
import com.klaus.moply.customers.application.usecase.FindCustomerById;
import com.klaus.moply.customers.application.usecase.FindCustomerLocationById;
import com.klaus.moply.customers.application.usecase.FindCustomerLocations;
import com.klaus.moply.customers.application.usecase.UpdateCustomer;
import com.klaus.moply.customers.application.usecase.UpdateCustomerLocation;
import com.klaus.moply.customers.application.usecase.dto.AddCustomerLocationInput;
import com.klaus.moply.customers.application.usecase.dto.FindCustomerLocationByIdInput;
import com.klaus.moply.customers.application.usecase.dto.UpdateCustomerInput;
import com.klaus.moply.customers.application.usecase.dto.UpdateCustomerLocationInput;
import com.klaus.moply.customers.infra.web.dto.CreateCustomerRequest;
import com.klaus.moply.customers.infra.web.dto.CustomerLocationRequest;
import com.klaus.moply.customers.infra.web.dto.CustomerLocationResponse;
import com.klaus.moply.customers.infra.web.dto.CustomerResponse;
import com.klaus.moply.customers.infra.web.dto.UpdateCustomerRequest;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

	private final CreateCustomer create;

	private final FindAllCustomers findAll;

	private final FindCustomerById findById;

	private final UpdateCustomer update;

	private final AddCustomerLocation addLocation;

	private final FindCustomerLocations findLocations;

	private final FindCustomerLocationById findLocation;

	private final UpdateCustomerLocation updateLocation;

	@PostMapping
	public ResponseEntity<UUID> create(@Valid @RequestBody CreateCustomerRequest request) {
		UUID id = create.execute(request.toInput());
		return ResponseEntity.created(URI.create("/api/v1/customers/" + id)).body(id);
	}

	@GetMapping
	public List<CustomerResponse> list() {
		return findAll.execute(null).stream().map(CustomerResponse::from).toList();
	}

	@GetMapping("/{customerId}")
	public CustomerResponse find(@PathVariable UUID customerId) {
		return CustomerResponse.from(findById.execute(customerId));
	}

	@PutMapping("/{customerId}")
	public CustomerResponse update(@PathVariable UUID customerId, @Valid @RequestBody UpdateCustomerRequest request) {
		return CustomerResponse.from(update.execute(new UpdateCustomerInput(customerId, request.name(), request.phone(),
				request.email(), request.notes())));
	}

	@PostMapping("/{customerId}/locations")
	public ResponseEntity<CustomerLocationResponse> addLocation(@PathVariable UUID customerId,
			@Valid @RequestBody CustomerLocationRequest request) {
		var output = addLocation
			.execute(new AddCustomerLocationInput(customerId, request.name(), request.address(), request.notes()));
		return ResponseEntity.created(URI.create("/api/v1/customers/" + customerId + "/locations/" + output.id()))
			.body(CustomerLocationResponse.from(output));
	}

	@GetMapping("/{customerId}/locations")
	public List<CustomerLocationResponse> locations(@PathVariable UUID customerId) {
		return findLocations.execute(customerId).stream().map(CustomerLocationResponse::from).toList();
	}

	@GetMapping("/{customerId}/locations/{locationId}")
	public CustomerLocationResponse location(@PathVariable UUID customerId, @PathVariable UUID locationId) {
		return CustomerLocationResponse
			.from(findLocation.execute(new FindCustomerLocationByIdInput(customerId, locationId)));
	}

	@PutMapping("/{customerId}/locations/{locationId}")
	public CustomerLocationResponse updateLocation(@PathVariable UUID customerId, @PathVariable UUID locationId,
			@Valid @RequestBody CustomerLocationRequest request) {
		return CustomerLocationResponse.from(updateLocation.execute(new UpdateCustomerLocationInput(customerId,
				locationId, request.name(), request.address(), request.notes())));
	}

}
