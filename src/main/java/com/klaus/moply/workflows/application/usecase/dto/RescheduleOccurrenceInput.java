package com.klaus.moply.workflows.application.usecase.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.klaus.moply.shared.application.usecase.exception.ApplicationException;

public record RescheduleOccurrenceInput(OccurrenceSelection selection, LocalDate serviceDate, LocalTime startTime) {
	public RescheduleOccurrenceInput {
		if (selection == null || serviceDate == null) {
			throw new ApplicationException("Seleção e nova data são obrigatórias.");
		}
	}
}
