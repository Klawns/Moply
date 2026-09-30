package com.klaus.moply.shared.domain.vo;

import java.math.BigDecimal;
import java.util.Currency;
import com.klaus.moply.shared.domain.exception.DomainException;

public record Money(BigDecimal value) {
	public Money {
		if (value == null) {
			throw new DomainException("Valor monetário é obrigatório.");
		}
		if (value.signum() < 0) {
			throw new DomainException("Valor monetário não pode ser negativo.");
		}
		if (value.scale() > 2) {
			throw new DomainException("Valor monetário deve ter no máximo duas casas decimais.");
		}
		value = value.setScale(2);
	}

	public Currency currency() {
		return Currency.getInstance("GBP");
	}
}
