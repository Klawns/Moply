package com.klaus.moply.customers.infra.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerLocationRequest(@NotBlank String name, String address, String notes) {
}
