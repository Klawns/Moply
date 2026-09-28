package com.klaus.moply.customers.infra.web.dto;

import java.util.List;
import java.util.UUID;

import com.klaus.moply.customers.application.usecase.dto.CustomerOutput;

public record CustomerResponse(UUID id, String name, String phone, String email, String notes,
		List<CustomerLocationResponse> locations) {
	public static CustomerResponse from(CustomerOutput output) {
		return new CustomerResponse(output.id(), output.name(), output.phone(), output.email(), output.notes(),
				output.locations().stream().map(CustomerLocationResponse::from).toList());
	}
}
