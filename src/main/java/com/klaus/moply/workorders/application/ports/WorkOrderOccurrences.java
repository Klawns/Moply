package com.klaus.moply.workorders.application.ports;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public interface WorkOrderOccurrences {

	record Reference(UUID id, UUID seriesId, LocalDate originalDate) {
	}

	Reference reference(UUID account, UUID workId);

	java.util.List<Reference> inSeries(UUID account, UUID seriesId);

	Set<LocalDate> findDates(UUID organizationId, UUID seriesId, LocalDate from, LocalDate until);

	void link(UUID organizationId, UUID workOrderId, UUID seriesId, LocalDate originalDate);

}
