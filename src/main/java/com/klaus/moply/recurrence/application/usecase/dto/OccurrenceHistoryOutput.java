package com.klaus.moply.recurrence.application.usecase.dto;

import java.time.*;
import java.util.UUID;

public record OccurrenceHistoryOutput(UUID operationId, UUID actorId, Instant at, String reason, long position,
		UUID targetSeriesId, LocalDate targetOccurrenceDate, UUID replacementWorkId, LocalDate serviceDateBefore,
		LocalDate serviceDateAfter) {
}
