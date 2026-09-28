package com.klaus.moply.customers.application.usecase.dto;

import java.util.UUID;

public record UpdateCustomerLocationInput(UUID customerId, UUID locationId, String name, String address, String notes) {
}
