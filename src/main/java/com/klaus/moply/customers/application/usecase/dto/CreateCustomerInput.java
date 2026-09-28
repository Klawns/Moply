package com.klaus.moply.customers.application.usecase.dto;

import java.util.List;

import com.klaus.moply.orderservice.domain.exception.DomainException;

import lombok.Builder;

@Builder
public record CreateCustomerInput(String name, String phone, String email, String notes,
		List<CustomerLocationInput> locations) {
	public CreateCustomerInput {
		if (locations != null && locations.stream().anyMatch(location -> location == null)) {
			throw new DomainException("A lista de locais não pode conter itens nulos.");
		}
		locations = locations == null ? List.of() : List.copyOf(locations);
	}

	public CreateCustomerInput(String name) {
		this(name, null, null, null);
	}

	public CreateCustomerInput(String name, String phone, String email, String notes) {
		this(name, phone, email, notes, List.of());
	}
}
