package com.klaus.moply.customers.infra.web.dto;

import java.util.UUID;
import com.klaus.moply.customers.application.usecase.dto.LocationSummary;

public record LocationSummaryResponse(UUID id, String name, String address, UUID customerId, String customerName) {
	public static LocationSummaryResponse from(LocationSummary value) {
		return new LocationSummaryResponse(value.id(), value.name(), value.address(), value.customerId(),
				value.customerName());
	}
}
