package com.klaus.moply.customers.application.usecase.dto;

import java.util.UUID;

public record LocationSummary(UUID id, String name, String address, UUID customerId, String customerName) {
}
