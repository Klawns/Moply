package com.klaus.moply.recurrence.infra.web.dto.response;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.klaus.moply.recurrence.domain.WorkTemplate;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

public record WorkConditionsResponse(UUID customerId, UUID customerLocationId, LocalTime startTime, String description,
		WorkConditionsPricingResponse pricing, List<UUID> participantIds, WorkOrderStatus initialStatus,
		FrozenWorkPricingResponse frozenPricing) {

	public static WorkConditionsResponse from(WorkTemplate t) {
		return new WorkConditionsResponse(t.customerId(), t.customerLocationId(), t.startTime(), t.description(),
				new WorkConditionsPricingResponse(t.contractedHours().value(), t.hourlyRate().value(),
						t.currencyCode()),
				t.participants().ids(), t.initialStatus(), FrozenWorkPricingResponse.from(t.frozenPricing()));
	}

}
