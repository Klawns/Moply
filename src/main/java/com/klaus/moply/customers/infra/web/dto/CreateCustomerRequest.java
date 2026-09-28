package com.klaus.moply.customers.infra.web.dto;

import java.util.List;

import com.klaus.moply.customers.application.usecase.dto.CreateCustomerInput;
import com.klaus.moply.customers.application.usecase.dto.CustomerLocationInput;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCustomerRequest(@NotBlank String name, String phone, String email, String notes,
		List<@NotNull @Valid CustomerLocationRequest> locations) {
	public CreateCustomerInput toInput() {
		return new CreateCustomerInput(name, phone, email, notes, locations == null ? List.of()
				: locations.stream().map(l -> new CustomerLocationInput(l.name(), l.address(), l.notes())).toList());
	}
}
