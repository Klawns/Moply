package com.klaus.moply.workorders.domain.vo;

import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.shared.domain.vo.Money;

/** Persisted commercial conditions; historical totals are never recalculated. */
public record WorkOrderPricing(DurationHours contractedHours, HourlyRate hourlyRate, String currencyCode,
		Money totalAmount, int allocationPolicyVersion) {
	public WorkOrderPricing {
		if (contractedHours == null || hourlyRate == null) {
			throw new DomainException("Horas e tarifa são obrigatórias.");
		}
		if (!"GBP".equals(currencyCode)) {
			throw new DomainException("A moeda do trabalho deve ser GBP.");
		}
		if (totalAmount == null || totalAmount.value().signum() <= 0) {
			throw new DomainException("O total do trabalho deve ser maior que zero.");
		}
		if (allocationPolicyVersion <= 0) {
			throw new DomainException("Versão da política de alocação deve ser positiva.");
		}
	}
}
