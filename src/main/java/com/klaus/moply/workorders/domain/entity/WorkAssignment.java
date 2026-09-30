package com.klaus.moply.workorders.domain.entity;

import java.util.UUID;

import com.klaus.moply.shared.domain.exception.DomainException;
import com.klaus.moply.shared.domain.vo.Money;

public record WorkAssignment(UUID collaboratorId, int inclusionPosition, Money allocatedAmount) {
	public WorkAssignment {
		if (collaboratorId == null || inclusionPosition < 0 || allocatedAmount == null)
			throw new DomainException("Participação inválida.");
	}
}
