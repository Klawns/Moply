package com.klaus.moply.workorders.application.usecase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

public record CreateWorkOrderInput(UUID customerId, UUID customerLocationId, LocalDate serviceDate, LocalTime startTime,
		String description, BigDecimal contractedHours, BigDecimal hourlyRate, List<UUID> participantIds,
		WorkOrderStatus initialStatus, String acceptedPricingFingerprint) {
	public CreateWorkOrderInput(UUID customerId, UUID customerLocationId, LocalDate serviceDate, LocalTime startTime,
			String description, BigDecimal contractedHours, BigDecimal hourlyRate, List<UUID> participantIds,
			WorkOrderStatus initialStatus) {
		this(customerId, customerLocationId, serviceDate, startTime, description, contractedHours, hourlyRate,
				participantIds, initialStatus, null);
	}
}
