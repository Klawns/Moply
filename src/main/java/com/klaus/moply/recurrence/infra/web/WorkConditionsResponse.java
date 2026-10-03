package com.klaus.moply.recurrence.infra.web;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.klaus.moply.recurrence.domain.WorkTemplate;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

public record WorkConditionsResponse(UUID customerId, UUID customerLocationId, LocalTime startTime, String description,
		BigDecimal contractedHours, BigDecimal hourlyRate, String currencyCode, List<UUID> participantIds,
		WorkOrderStatus initialStatus) {
	static WorkConditionsResponse from(WorkTemplate template) {
		return new WorkConditionsResponse(template.customerId(), template.customerLocationId(), template.startTime(),
				template.description(), template.contractedHours().value(), template.hourlyRate().value(),
				template.currencyCode(), template.participants().ids(), template.initialStatus());
	}
}
