package com.klaus.moply.customers.application.usecase.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record CreateCustomerInput(String name, String phone, String email, String notes,
		List<CustomerLocationInput> locations) {
}