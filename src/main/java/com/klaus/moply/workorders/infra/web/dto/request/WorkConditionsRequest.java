package com.klaus.moply.workorders.infra.web.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

import jakarta.validation.constraints.NotNull;

public record WorkConditionsRequest(@NotNull UUID customerId, UUID customerLocationId, LocalTime startTime,
		String description, @NotNull BigDecimal contractedHours, BigDecimal hourlyRate,
		@NotNull List<@NotNull UUID> participantIds, WorkOrderStatus initialStatus) {

	public CreateWorkOrderInput toInput(LocalDate serviceDate, String acceptedPricingFingerprint) {
		return new CreateWorkOrderInput(customerId, customerLocationId, serviceDate, startTime, description,
				contractedHours, hourlyRate, participantIds, initialStatus, acceptedPricingFingerprint);
	}

}
