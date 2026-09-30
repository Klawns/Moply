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
import com.klaus.moply.customers.application.usecase.CreateCustomer;
import com.klaus.moply.customers.application.usecase.FindAllCustomers;
import com.klaus.moply.customers.application.usecase.FindCustomerById;
import com.klaus.moply.customers.application.usecase.UpdateCustomer;
import com.klaus.moply.customers.application.usecase.dto.UpdateCustomerInput;
import com.klaus.moply.customers.infra.web.dto.CreateCustomerRequest;
import com.klaus.moply.customers.infra.web.dto.CustomerResponse;
import com.klaus.moply.customers.infra.web.dto.UpdateCustomerRequest;
import com.klaus.moply.shared.application.usecase.Usecase.Context;

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

	@PostMapping
	public ResponseEntity<UUID> create(@AuthenticationPrincipal AccountPrincipal principal,
			@Valid @RequestBody CreateCustomerRequest request) {
		UUID id = create.execute(new Context(principal.getOrganizationId()), request.toInput());
		return ResponseEntity.created(URI.create("/api/v1/customers/" + id)).body(id);
	}

	@GetMapping
	public List<CustomerResponse> list(@AuthenticationPrincipal AccountPrincipal principal) {
		return findAll.execute(new Context(principal.getOrganizationId()), null)
			.stream()
			.map(CustomerResponse::from)
			.toList();
	}

	@GetMapping("/{customerId}")
	public CustomerResponse find(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID customerId) {
		return CustomerResponse.from(findById.execute(new Context(principal.getOrganizationId()), customerId));
	}

	@PutMapping("/{customerId}")
	public CustomerResponse update(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable UUID customerId,
			@Valid @RequestBody UpdateCustomerRequest request) {
		return CustomerResponse
			.from(update.execute(new Context(principal.getOrganizationId()), new UpdateCustomerInput(customerId,
					request.name(), request.phone(), request.email(), request.notes())));
	}

}
