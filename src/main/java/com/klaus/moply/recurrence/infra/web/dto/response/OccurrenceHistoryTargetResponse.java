package com.klaus.moply.recurrence.infra.web.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record OccurrenceHistoryTargetResponse(long position, UUID seriesId, LocalDate occurrenceDate) {
}
