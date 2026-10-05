package com.klaus.moply.recurrence.infra.web;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.klaus.moply.recurrence.domain.WorkTemplate;
import com.klaus.moply.recurrence.domain.FrozenWorkPricing;
import com.klaus.moply.workorders.application.usecase.dto.WorkOrderOutput.AssignmentOutput;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

public record WorkConditionsResponse(UUID customerId, UUID customerLocationId, LocalTime startTime, String description,
		BigDecimal contractedHours, BigDecimal hourlyRate, String currencyCode, List<UUID> participantIds,
		WorkOrderStatus initialStatus, FrozenPricingResponse frozenPricing) {

	static WorkConditionsResponse from(WorkTemplate template) {
		return new WorkConditionsResponse(template.customerId(), template.customerLocationId(), template.startTime(),
				template.description(), template.contractedHours().value(), template.hourlyRate().value(),
				template.currencyCode(), template.participants().ids(), template.initialStatus(),
				FrozenPricingResponse.from(template.frozenPricing()));
	}
	public record FrozenPricingResponse(BigDecimal totalAmount, int allocationPolicyVersion,
			List<AssignmentOutput> assignments) {
		static FrozenPricingResponse from(FrozenWorkPricing frozen) {
			return frozen == null ? null
					: new FrozenPricingResponse(frozen.pricing().totalAmount().value(),
							frozen.pricing().allocationPolicyVersion(),
							frozen.assignments().values().stream().map(AssignmentOutput::from).toList());
		}
	}
}
