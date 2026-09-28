package com.klaus.moply.customers.infra.web.dto;

import java.util.UUID;

import com.klaus.moply.customers.application.usecase.dto.CustomerLocationOutput;

public record CustomerLocationResponse(UUID id, String name, String address, String notes) {
	public static CustomerLocationResponse from(CustomerLocationOutput output) {
		return new CustomerLocationResponse(output.id(), output.name(), output.address(), output.notes());
	}
}
