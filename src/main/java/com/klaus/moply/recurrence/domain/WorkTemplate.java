package com.klaus.moply.recurrence.domain;

import java.time.LocalTime;
import java.util.UUID;

import com.klaus.moply.recurrence.domain.vo.RecurrenceParticipants;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.workorders.domain.entity.WorkOrderStatus;
import com.klaus.moply.workorders.domain.vo.DurationHours;
import com.klaus.moply.workorders.domain.vo.HourlyRate;

public record WorkTemplate(UUID customerId, UUID customerLocationId, LocalTime startTime, String description,
		DurationHours contractedHours, HourlyRate hourlyRate, String currencyCode, RecurrenceParticipants participants,
		WorkOrderStatus initialStatus, FrozenWorkPricing frozenPricing) {
	public WorkTemplate(UUID customerId, UUID customerLocationId, LocalTime startTime, String description,
			DurationHours contractedHours, HourlyRate hourlyRate, String currencyCode,
			RecurrenceParticipants participants, WorkOrderStatus initialStatus) {
		this(customerId, customerLocationId, startTime, description, contractedHours, hourlyRate, currencyCode,
				participants, initialStatus, null);
	}

	public WorkTemplate {
		validateRequiredConditions(customerId, contractedHours, hourlyRate);
		validateCurrency(currencyCode);
		validateParticipants(participants);
		validateInitialStatus(initialStatus);
		if (frozenPricing != null && (!frozenPricing.pricing().contractedHours().equals(contractedHours)
				|| !frozenPricing.pricing().hourlyRate().equals(hourlyRate)
				|| !frozenPricing.pricing().currencyCode().equals(currencyCode)
				|| !frozenPricing.assignments()
					.values()
					.stream()
					.map(a -> a.collaboratorId())
					.toList()
					.equals(participants.ids())))
			throw new DomainException("Condições financeiras não correspondem à série.");
	}

	private static void validateRequiredConditions(UUID customerId, DurationHours contractedHours,
			HourlyRate hourlyRate) {
		if (customerId == null || contractedHours == null || hourlyRate == null) {
			throw new DomainException("Condições da série inválidas.");
		}
	}

	private static void validateCurrency(String currencyCode) {
		if (!"GBP".equals(currencyCode)) {
			throw new DomainException("Condições da série inválidas.");
		}
	}

	private static void validateParticipants(RecurrenceParticipants participants) {
		if (participants == null) {
			throw new DomainException("Condições da série inválidas.");
		}
	}

	private static void validateInitialStatus(WorkOrderStatus initialStatus) {
		if (initialStatus == null || initialStatus == WorkOrderStatus.CANCELLED)
			throw new DomainException("Condições da série inválidas.");
	}
}
