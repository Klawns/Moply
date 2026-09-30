package com.klaus.moply.workorders.domain.vo;

import java.math.BigDecimal;

import com.klaus.moply.shared.domain.exception.DomainException;

public record DurationHours(BigDecimal value) {
	public DurationHours {
		if (value == null) {
			throw new DomainException("Horas são obrigatórias.");
		}
		if (value.signum() <= 0) {
			throw new DomainException("Horas devem ser maiores que zero.");
		}
		if (value.scale() > 2) {
			throw new DomainException("Horas devem ter no máximo duas casas decimais.");
		}
		value = value.setScale(2);
	}
}
