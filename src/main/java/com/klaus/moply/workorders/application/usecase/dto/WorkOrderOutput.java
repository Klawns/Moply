package com.klaus.moply.workorders.application.usecase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.klaus.moply.workorders.domain.entity.WorkOrder;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;

public record WorkOrderOutput(UUID id, UUID customerId, String customer, UUID customerLocationId, LocalDate serviceDate,
		LocalTime startTime, String description, BigDecimal contractedHours, BigDecimal hourlyRate, String currencyCode,
		BigDecimal totalAmount, int allocationPolicyVersion, WorkOrderStatus status, long version, int participantCount,
		List<AssignmentOutput> assignments, UUID recurrenceSeriesId, LocalDate occurrenceDate) {
	public record AssignmentOutput(UUID collaboratorId, int inclusionPosition, BigDecimal allocatedAmount) {
	}

	public static WorkOrderOutput from(WorkOrder w, String customer) {
		return new WorkOrderOutput(w.id(), w.customerId(), customer, w.customerLocationId(), w.serviceDate(),
				w.startTime(), w.description(), w.contractedHours().value(), w.hourlyRate().value(), w.currencyCode(),
				w.totalAmount().value(), w.allocationPolicyVersion(), w.status(), w.version(), w.participantCount(),
				w.assignments()
					.stream()
					.map(a -> new AssignmentOutput(a.collaboratorId(), a.inclusionPosition(),
							a.allocatedAmount().value()))
					.toList(),
				w.occurrence() == null ? null : w.occurrence().seriesId(),
				w.occurrence() == null ? null : w.occurrence().originalDate());
	}
}
