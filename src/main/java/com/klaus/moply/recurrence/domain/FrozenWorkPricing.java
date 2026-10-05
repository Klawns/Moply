package com.klaus.moply.recurrence.domain;

import com.klaus.moply.workorders.domain.vo.WorkOrderPricing;
import com.klaus.moply.workorders.domain.vo.WorkOrderAssignments;
import com.klaus.moply.shared.domain.exception.DomainException;

/** Financial conditions approved once and reused for every occurrence. */
public record FrozenWorkPricing(WorkOrderPricing pricing, WorkOrderAssignments assignments) {
	public FrozenWorkPricing {
		if (pricing == null || assignments == null || !pricing.totalAmount().equals(assignments.totalAmount()))
			throw new DomainException("Condições financeiras da série inválidas.");
	}
}
