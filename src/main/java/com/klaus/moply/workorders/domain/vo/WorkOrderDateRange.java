package com.klaus.moply.workorders.domain.vo;

import java.time.LocalDate;
import java.util.Objects;
import com.klaus.moply.shared.domain.exception.DomainException;

/** Optional, inclusive agenda bounds. */
public record WorkOrderDateRange(LocalDate from, LocalDate to) {
	public WorkOrderDateRange {
		if (from != null && to != null && from.isAfter(to))
			throw new DomainException("Intervalo de datas invertido.");
	}

	public boolean contains(LocalDate date) {
		Objects.requireNonNull(date);
		return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
	}
}
