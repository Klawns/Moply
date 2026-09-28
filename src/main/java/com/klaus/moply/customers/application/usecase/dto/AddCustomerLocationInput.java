package com.klaus.moply.customers.application.usecase.dto;

import java.util.UUID;

public record AddCustomerLocationInput(UUID customerId, String name, String address, String notes) {
}
