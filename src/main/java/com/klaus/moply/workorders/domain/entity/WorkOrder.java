package com.klaus.moply.workorders.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.shared.domain.vo.Money;
import com.klaus.moply.workorders.domain.ExactAllocationPolicy;
import com.klaus.moply.workorders.domain.exception.WorkOrderStateException;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;

public record WorkOrder(UUID id, UUID customerId, UUID customerLocationId, LocalDate serviceDate, LocalTime startTime,
		String description, DurationHours contractedHours, HourlyRate hourlyRate, String currencyCode,
		Money totalAmount, int allocationPolicyVersion, WorkOrderStatus status, long version,
		List<WorkAssignment> assignments) {
	public WorkOrder {
		if (customerId == null || serviceDate == null || contractedHours == null || hourlyRate == null
				|| !"GBP".equals(currencyCode) || totalAmount == null || totalAmount.value().signum() <= 0
				|| allocationPolicyVersion <= 0 || status == null || version < 0 || assignments == null
				|| assignments.isEmpty())
			throw new DomainException("Trabalho inválido.");
		var ids = new HashSet<UUID>();
		BigDecimal sum = BigDecimal.ZERO;
		for (int i = 0; i < assignments.size(); i++) {
			var assignment = assignments.get(i);
			if (assignment == null || assignment.inclusionPosition() != i || !ids.add(assignment.collaboratorId()))
				throw new DomainException("Participações devem ser únicas e ordenadas a partir de zero.");
			sum = sum.add(assignment.allocatedAmount().value());
		}
		if (sum.compareTo(totalAmount.value()) != 0)
			throw new DomainException("Parcelas devem somar o total persistido.");
		assignments = List.copyOf(assignments);
		description = description == null || description.isBlank() ? null : description.strip();
	}

	public static WorkOrder create(UUID customerId, UUID locationId, LocalDate date, LocalTime time, String description,
			BigDecimal hours, BigDecimal rate, List<UUID> participants, WorkOrderStatus status) {
		if (status == null || status == WorkOrderStatus.CANCELLED)
			throw new DomainException("Estado inicial inválido.");
		var calculation = new ExactAllocationPolicy().calculate(new DurationHours(hours), new HourlyRate(rate),
				participants);
		return new WorkOrder(null, customerId, locationId, date, time, description, calculation.hours(),
				calculation.hourlyRate(), "GBP", calculation.total(), calculation.policyVersion(), status, 0,
				calculation.allocations()
					.stream()
					.map(a -> new WorkAssignment(a.participantId(), a.inclusionPosition(), a.amount()))
					.toList());
	}

	public WorkOrder complete() {
		requireActive();
		return status == WorkOrderStatus.COMPLETED ? this
				: withOperation(serviceDate, startTime, WorkOrderStatus.COMPLETED);
	}

	public WorkOrder cancel() {
		return status == WorkOrderStatus.CANCELLED ? this
				: withOperation(serviceDate, startTime, WorkOrderStatus.CANCELLED);
	}

	public WorkOrder reschedule(LocalDate date, LocalTime time, LocalDate today) {
		requireActive();
		if (date == null || today == null)
			throw new DomainException("Data obrigatória.");
		if (status == WorkOrderStatus.COMPLETED && !serviceDate.isAfter(today))
			throw new WorkOrderStateException("Trabalho concluído cuja data já chegou não pode ser reagendado.");
		return withOperation(date, time, status);
	}

	private void requireActive() {
		if (status == WorkOrderStatus.CANCELLED)
			throw new WorkOrderStateException("Trabalho cancelado não pode ser alterado.");
	}

	private WorkOrder withOperation(LocalDate date, LocalTime time, WorkOrderStatus state) {
		return new WorkOrder(id, customerId, customerLocationId, date, time, description, contractedHours, hourlyRate,
				currencyCode, totalAmount, allocationPolicyVersion, state, version, assignments);
	}

	public int participantCount() {
		return assignments.size();
	}
}
