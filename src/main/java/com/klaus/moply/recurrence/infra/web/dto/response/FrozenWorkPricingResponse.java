package com.klaus.moply.recurrence.infra.web.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.klaus.moply.recurrence.domain.FrozenWorkPricing;
import com.klaus.moply.workorders.infra.web.dto.response.WorkAssignmentResponse;

public record FrozenWorkPricingResponse(BigDecimal totalAmount, int allocationPolicyVersion,
		List<WorkAssignmentResponse> assignments) {

	public static FrozenWorkPricingResponse from(FrozenWorkPricing f) {
		return f == null ? null
				: new FrozenWorkPricingResponse(f.pricing().totalAmount().value(),
						f.pricing().allocationPolicyVersion(),
						f.assignments().values().stream().map(WorkAssignmentResponse::from).toList());
	}

}
