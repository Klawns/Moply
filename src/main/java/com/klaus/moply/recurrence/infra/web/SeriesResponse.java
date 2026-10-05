package com.klaus.moply.recurrence.infra.web;

import java.time.LocalDate;
import java.util.UUID;
import com.klaus.moply.recurrence.domain.*;

public record SeriesResponse(UUID id, Frequency frequency, LocalDate startsOn, LocalDate endsOn,
		WorkConditionsResponse conditions, UUID familyId, UUID previousSeriesId, long firstPosition,
		Long untilPosition) {
	static SeriesResponse from(RecurrenceSeries series) {
		return new SeriesResponse(series.getId(), series.getFrequency(), series.getPeriod().startsOn(),
				series.getPeriod().endsOn(), WorkConditionsResponse.from(series.getTemplate()),
				series.getLineage().familyId(), series.getLineage().previousSeriesId(),
				series.getLineage().firstPosition(), series.getLineage().untilPosition());
	}
}
