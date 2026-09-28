package com.klaus.moply.customers.application.usecase.dto;

import java.util.UUID;

public record FindCustomerLocationByIdInput(UUID customerId, UUID locationId) {
}
