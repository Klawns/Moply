package com.klaus.moply.orderservice.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.klaus.moply.orderservice.domain.exception.DomainException;

public record DurationHours(BigDecimal value) {
	public DurationHours {
		if (value == null) {
			throw new DomainException("Hours can't be null.");
		}

		if (value.compareTo(BigDecimal.ZERO) <= 0) {
			throw new DomainException("Hours must be greater than zero.");
		}

		value = value.setScale(2, RoundingMode.HALF_UP);
	}

	public DurationHours divide(Integer divisor) {
		if (divisor == null || divisor <= 0) {
			throw new DomainException("Divisor must be greater than zero.");
		}

		BigDecimal result = value.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);

		return new DurationHours(result);
	}
}
