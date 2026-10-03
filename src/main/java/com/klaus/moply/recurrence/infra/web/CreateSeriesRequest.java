package com.klaus.moply.recurrence.infra.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import com.klaus.moply.recurrence.domain.Frequency;
import com.klaus.moply.recurrence.application.usecase.dto.CreateSeriesInput;
import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

public record CreateSeriesRequest(Frequency frequency, LocalDate startsOn, LocalDate endsOn, UUID customerId,
		UUID customerLocationId, LocalTime startTime, String description, BigDecimal contractedHours,
		BigDecimal hourlyRate, List<UUID> participantIds, WorkOrderStatus initialStatus) {
	CreateSeriesInput toInput() {
		return new CreateSeriesInput(frequency, startsOn, endsOn,
				new CreateWorkOrderInput(customerId, customerLocationId, startsOn, startTime, description,
						contractedHours, hourlyRate, participantIds, initialStatus));
	}
}
