package com.klaus.moply.workflows.application.usecase.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.klaus.moply.shared.domain.exception.DomainException;

public record RescheduleOccurrenceInput(OccurrenceSelection selection, LocalDate serviceDate, LocalTime startTime) {
	public RescheduleOccurrenceInput {
		if (selection == null || serviceDate == null) {
			throw new DomainException("Seleção e nova data são obrigatórias.");
		}
	}
}
