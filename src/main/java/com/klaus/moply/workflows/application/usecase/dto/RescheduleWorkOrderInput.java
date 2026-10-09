package com.klaus.moply.workflows.application.usecase.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.klaus.moply.shared.application.usecase.exception.ApplicationException;

public record RescheduleWorkOrderInput(UUID id, LocalDate serviceDate, LocalTime startTime) {
	public RescheduleWorkOrderInput {
		if (id == null || serviceDate == null) {
			throw new ApplicationException("Trabalho e data obrigatórios.");
		}
	}
}
