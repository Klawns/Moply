package com.klaus.moply.recurrence.application.usecase.dto;

import java.time.LocalDate;
import java.util.UUID;

public record GenerateSeriesResult(UUID organizationId, UUID seriesId, LocalDate from, LocalDate until, int created,
		int existing) {
}
