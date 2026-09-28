package com.klaus.moply.customers.application.usecase.dto;

import java.util.List;
import java.util.UUID;

import com.klaus.moply.customers.domain.entities.Customer;

import lombok.Builder;

@Builder
public record CustomerOutput(
		UUID id,
		String name,
		String phone,
		String email,
		String notes,
		List<CustomerLocationOutput> locations) {

	public static CustomerOutput fromDomain(Customer customer) {
		return new CustomerOutput(
				customer.getId(),
				customer.getName().value(),
				customer.getPhone() == null ? null : customer.getPhone().value(),
				customer.getEmail() == null ? null : customer.getEmail().value(),
				customer.getNotes(),
				customer.getLocations().stream()
						.map(CustomerLocationOutput::fromDomain)
						.toList());
	}
}