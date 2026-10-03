package com.klaus.moply.payments.domain;

import java.math.BigDecimal;

import com.klaus.moply.shared.domain.exception.DomainException;

public record PaymentAmount(BigDecimal value, String currencyCode) {

	public PaymentAmount {
		if (value == null || value.signum() <= 0 || value.scale() > 2)
			throw new DomainException("Pagamento deve ter valor positivo com até duas casas decimais.");
		if (!"GBP".equals(currencyCode))
			throw new DomainException("Moeda de pagamento inválida.");
		value = value.setScale(2);
	}

}
