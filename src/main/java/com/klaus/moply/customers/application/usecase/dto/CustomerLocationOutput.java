package com.klaus.moply.customers.application.usecase.dto;

import java.util.UUID;

import com.klaus.moply.customers.domain.entities.CustomerLocation;

public record CustomerLocationOutput(UUID id, String name, String address, String notes) {
	public static CustomerLocationOutput fromDomain(CustomerLocation location) {
		return new CustomerLocationOutput(location.getId(), location.getName(), location.getAddress(),
				location.getNotes());
	}
}
