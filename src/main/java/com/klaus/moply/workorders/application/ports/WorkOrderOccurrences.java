package com.klaus.moply.workorders.application.ports;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Public occurrence identity contract; callers must create and link in one transaction.
 */
public interface WorkOrderOccurrences {

	Set<LocalDate> findDates(UUID organizationId, UUID seriesId, LocalDate from, LocalDate until);

	void link(UUID organizationId, UUID workOrderId, UUID seriesId, LocalDate originalDate);

}
