package com.klaus.moply.workflows.application.usecase.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.klaus.moply.shared.domain.exception.DomainException;

public record RescheduleWorkOrderInput(UUID id, LocalDate serviceDate, LocalTime startTime) {
	public RescheduleWorkOrderInput {
		if (id == null || serviceDate == null) {
			throw new DomainException("Trabalho e data obrigatórios.");
		}
	}
}
