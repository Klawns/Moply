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
		WorkOrderStatus initialStatus) {
	public WorkTemplate {
		validateRequiredConditions(customerId, contractedHours, hourlyRate);
		validateCurrency(currencyCode);
		validateParticipants(participants);
		validateInitialStatus(initialStatus);
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
		if (initialStatus == null || initialStatus == WorkOrderStatus.CANCELLED) {
			throw new DomainException("Condições da série inválidas.");
		}

	}

}
