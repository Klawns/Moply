package com.klaus.moply.customers.infra.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCustomerRequest(@NotBlank String name, String phone, String email, String notes) {
}
