package com.klaus.moply.customers.application.usecase.dto;

import java.util.UUID;
import java.util.List;
import com.klaus.moply.customers.domain.Customer;
import lombok.Builder;

@Builder
public record CustomerOutput(UUID id, String name, String phone, String email, String notes,
		List<CustomerLocationOutput> locations) {
	public CustomerOutput {
		locations = locations == null ? List.of() : List.copyOf(locations);
	}

	public CustomerOutput(UUID id, String name, String phone, String email, String notes) {
		this(id, name, phone, email, notes, List.of());
	}

	public static CustomerOutput fromDomain(Customer customer) {
		return new CustomerOutput(customer.getId(), customer.getName().value(),
				customer.getPhone() == null ? null : customer.getPhone().value(),
				customer.getEmail() == null ? null : customer.getEmail().value(), customer.getNotes(),
				customer.getLocations().stream().map(CustomerLocationOutput::fromDomain).toList());
	}
}
