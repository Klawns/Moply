package com.klaus.moply.workflows.application.usecase.dto;

import java.util.List;
import java.util.UUID;

import com.klaus.moply.recurrence.domain.RecurrenceSeries;

public record SelectionContext(OccurrenceSelection selection, UUID organizationId, RecurrenceSeries anchor,
		long fromPosition, List<RecurrenceSeries> versions) {
	public SelectionContext {
		versions = List.copyOf(versions);
	}
}
