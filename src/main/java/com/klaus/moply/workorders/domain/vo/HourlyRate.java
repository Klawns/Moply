package com.klaus.moply.workorders.domain.vo;

import java.math.BigDecimal;

import com.klaus.moply.shared.domain.exception.DomainException;

public record HourlyRate(BigDecimal value) {
	public HourlyRate {
		if (value == null) {
			throw new DomainException("Tarifa por hora é obrigatória.");
		}
		if (value.signum() <= 0) {
			throw new DomainException("Tarifa por hora deve ser maior que zero.");
		}
		if (value.scale() > 2) {
			throw new DomainException("Tarifa por hora deve ter no máximo duas casas decimais.");
		}
		value = value.setScale(2);
	}
}
