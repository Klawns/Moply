package com.klaus.moply.workorders.domain.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.shared.domain.vo.Money;
import com.klaus.moply.workorders.domain.exception.WorkOrderStateException;
import com.klaus.moply.workorders.domain.policy.ExactAllocationPolicy;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;
import com.klaus.moply.workorders.domain.vo.OccurrenceIdentity;
import com.klaus.moply.workorders.domain.vo.WorkOrderAssignments;
import com.klaus.moply.workorders.domain.vo.WorkOrderDescription;
import com.klaus.moply.workorders.domain.vo.WorkOrderPricing;
import com.klaus.moply.workorders.domain.vo.WorkOrderSchedule;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class WorkOrder {

	private final UUID id;

	private final UUID customerId;

	private final UUID customerLocationId;

	private final WorkOrderSchedule schedule;

	private final WorkOrderDescription workDescription;

	private final WorkOrderPricing pricing;

	private final WorkOrderStatus status;

	private final long version;

	private final WorkOrderAssignments workAssignments;

	private final OccurrenceIdentity occurrence;

	private WorkOrder(UUID id, UUID customerId, UUID customerLocationId, WorkOrderSchedule schedule,
			WorkOrderDescription workDescription, WorkOrderPricing pricing, WorkOrderStatus status, long version,
			WorkOrderAssignments workAssignments, OccurrenceIdentity occurrence) {
		if (customerId == null || schedule == null || workDescription == null || pricing == null || status == null
				|| workAssignments == null) {
			throw new DomainException("Dados do trabalho incompletos.");
		}
		if (version < 0) {
			throw new DomainException("Versão do trabalho não pode ser negativa.");
		}
		if (!workAssignments.totalAmount().equals(pricing.totalAmount())) {
			throw new DomainException("Parcelas devem somar o total persistido.");
		}
		this.id = id;
		this.customerId = customerId;
		this.customerLocationId = customerLocationId;
		this.schedule = schedule;
		this.workDescription = workDescription;
		this.pricing = pricing;
		this.status = status;
		this.version = version;
		this.workAssignments = workAssignments;
		this.occurrence = occurrence;
	}

	public static WorkOrder create(UUID customerId, UUID locationId, WorkOrderSchedule schedule,
			WorkOrderDescription workDescription, DurationHours hours, HourlyRate rate, List<UUID> participants,
			WorkOrderStatus status) {
		if (status == null || status == WorkOrderStatus.CANCELLED) {
			throw new DomainException("Estado inicial inválido.");
		}
		var calculation = new ExactAllocationPolicy().calculate(hours, rate, participants);
		var pricing = new WorkOrderPricing(hours, rate, calculation.total().currency().getCurrencyCode(),
				calculation.total(), calculation.policyVersion());
		var workAssignments = new WorkOrderAssignments(calculation.allocations()
			.stream()
			.map(a -> new WorkAssignment(a.participantId(), a.inclusionPosition(), a.amount()))
			.toList());
		return new WorkOrder(null, customerId, locationId, schedule, workDescription, pricing, status, 0,
				workAssignments, null);
	}

	/**
	 * Restores and validates historical conditions without applying the current policy.
	 */
	public static WorkOrder restore(UUID id, UUID customerId, UUID customerLocationId, WorkOrderSchedule schedule,
			WorkOrderDescription workDescription, WorkOrderPricing pricing, WorkOrderStatus status, long version,
			WorkOrderAssignments workAssignments, OccurrenceIdentity occurrence) {
		if (id == null) {
			throw new DomainException("O ID do trabalho é obrigatório para reconstituição.");
		}
		return new WorkOrder(id, customerId, customerLocationId, schedule, workDescription, pricing, status, version,
				workAssignments, occurrence);
	}

	public WorkOrder complete() {
		requireActive();
		return status == WorkOrderStatus.COMPLETED ? this : withOperation(schedule, WorkOrderStatus.COMPLETED);
	}

	public WorkOrder cancel() {
		return status == WorkOrderStatus.CANCELLED ? this : withOperation(schedule, WorkOrderStatus.CANCELLED);
	}

	public WorkOrder reschedule(LocalDate date, LocalTime time, LocalDate today) {
		requireActive();
		var newSchedule = new WorkOrderSchedule(date, time);
		if (today == null) {
			throw new DomainException("Data atual é obrigatória.");
		}
		if (status == WorkOrderStatus.COMPLETED && !serviceDate().isAfter(today)) {
			throw new WorkOrderStateException("Trabalho concluído cuja data já chegou não pode ser reagendado.");
		}
		return withOperation(newSchedule, status);
	}

	/** Compares identity and conditions that operational transitions must preserve. */
	public boolean hasSameConditionsAs(WorkOrder other) {
		return other != null && Objects.equals(id, other.id) && customerId.equals(other.customerId)
				&& Objects.equals(customerLocationId, other.customerLocationId)
				&& workDescription.equals(other.workDescription) && pricing.equals(other.pricing)
				&& version == other.version && workAssignments.equals(other.workAssignments)
				&& Objects.equals(occurrence, other.occurrence);
	}

	public boolean hasSameOperationAs(WorkOrder other) {
		return other != null && schedule.equals(other.schedule) && status == other.status;
	}

	private void requireActive() {
		if (status == WorkOrderStatus.CANCELLED) {
			throw new WorkOrderStateException("Trabalho cancelado não pode ser alterado.");
		}
	}

	private WorkOrder withOperation(WorkOrderSchedule schedule, WorkOrderStatus status) {
		return new WorkOrder(id, customerId, customerLocationId, schedule, workDescription, pricing, status, version,
				workAssignments, occurrence);
	}

	public LocalDate serviceDate() {
		return schedule.serviceDate();
	}

	public LocalTime startTime() {
		return schedule.startTime();
	}

	public String description() {
		return workDescription.value();
	}

	public DurationHours contractedHours() {
		return pricing.contractedHours();
	}

	public HourlyRate hourlyRate() {
		return pricing.hourlyRate();
	}

	public String currencyCode() {
		return pricing.currencyCode();
	}

	public Money totalAmount() {
		return pricing.totalAmount();
	}

	public int allocationPolicyVersion() {
		return pricing.allocationPolicyVersion();
	}

	public List<WorkAssignment> assignments() {
		return workAssignments.values();
	}

	public int participantCount() {
		return workAssignments.count();
	}

}
