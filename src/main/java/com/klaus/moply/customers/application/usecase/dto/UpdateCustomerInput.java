package com.klaus.moply.customers.application.usecase.dto;

import java.util.UUID;

public record UpdateCustomerInput(UUID id, String name, String phone, String email, String notes) {
}
