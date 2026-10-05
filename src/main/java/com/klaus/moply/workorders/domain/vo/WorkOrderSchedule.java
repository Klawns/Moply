package com.klaus.moply.workorders.domain.vo;

import java.time.LocalDate;
import java.time.LocalTime;

import com.klaus.moply.shared.domain.exception.DomainException;

public record WorkOrderSchedule(LocalDate serviceDate, LocalTime startTime) {
	public WorkOrderSchedule {
		if (serviceDate == null) {
			throw new DomainException("Data do trabalho é obrigatória.");
		}
	}
}
