package com.klaus.moply.workorders.infra.web.dto.response;

import java.util.List;
import java.util.UUID;

import com.klaus.moply.shared.infra.web.dto.response.CustomerReferenceResponse;
import com.klaus.moply.workorders.application.usecase.dto.WorkOrderOutput;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

public record WorkOrderResponse(UUID id, String description, WorkOrderStatus status, long version, int participantCount,
		CustomerReferenceResponse customer, UUID customerLocationId, WorkOrderScheduleResponse schedule,
		WorkPricingResponse pricing, List<WorkAssignmentResponse> assignments, WorkOrderRecurrenceResponse recurrence) {

	public static WorkOrderResponse from(WorkOrderOutput w) {
		return new WorkOrderResponse(w.id(), w.description(), w.status(), w.version(), w.participantCount(),
				new CustomerReferenceResponse(w.customerId(), w.customer()), w.customerLocationId(),
				new WorkOrderScheduleResponse(w.serviceDate(), w.startTime()),
				new WorkPricingResponse(w.contractedHours(), w.hourlyRate(), w.currencyCode(), w.totalAmount(),
						w.allocationPolicyVersion()),
				w.assignments().stream().map(WorkAssignmentResponse::from).toList(), w.recurrenceSeriesId() == null
						? null : new WorkOrderRecurrenceResponse(w.recurrenceSeriesId(), w.occurrenceDate()));
	}

}
