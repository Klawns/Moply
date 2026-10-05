package com.klaus.moply.workorders.domain.entity;

import java.util.UUID;
import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.shared.domain.vo.Money;
import com.klaus.moply.workorders.domain.vo.HourlyRate;

public record WorkAssignment(UUID collaboratorId, int inclusionPosition, Money allocatedAmount,
		HourlyRate appliedHourlyRate, Boolean fixedRate, Money baseAmount, Money surplusAmount) {
	public WorkAssignment(UUID collaboratorId, int inclusionPosition, Money allocatedAmount) {
		this(collaboratorId, inclusionPosition, allocatedAmount, null, null, null, null);
	}

	public WorkAssignment {
		if (collaboratorId == null || inclusionPosition < 0 || allocatedAmount == null)
			throw new DomainException("Participação inválida.");
		boolean legacy = appliedHourlyRate == null && fixedRate == null && baseAmount == null && surplusAmount == null;
		if (!legacy && (appliedHourlyRate == null || fixedRate == null || baseAmount == null || surplusAmount == null
				|| !baseAmount.value().add(surplusAmount.value()).equals(allocatedAmount.value())))
			throw new DomainException("Condições da remuneração inválidas.");
	}
}
