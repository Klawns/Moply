package com.klaus.moply.workorders.domain.vo;

import java.time.LocalDate;
import java.util.UUID;
import com.klaus.moply.shared.domain.exception.DomainException;

public record OccurrenceIdentity(UUID seriesId, LocalDate originalDate) {
	public OccurrenceIdentity {
		if (seriesId == null || originalDate == null)
			throw new DomainException("Identidade da ocorrência inválida.");
	}
}
