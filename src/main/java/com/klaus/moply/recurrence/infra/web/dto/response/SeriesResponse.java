package com.klaus.moply.recurrence.infra.web.dto.response;

import java.util.UUID;

import com.klaus.moply.recurrence.domain.Frequency;
import com.klaus.moply.recurrence.domain.RecurrenceSeries;
import com.klaus.moply.shared.infra.web.dto.response.DatePeriodResponse;

public record SeriesResponse(UUID id, Frequency frequency, DatePeriodResponse period, WorkConditionsResponse conditions,
		SeriesLineageResponse lineage) {

	public static SeriesResponse from(RecurrenceSeries s) {
		return new SeriesResponse(s.getId(), s.getFrequency(),
				new DatePeriodResponse(s.getPeriod().startsOn(), s.getPeriod().endsOn()),
				WorkConditionsResponse.from(s.getTemplate()),
				new SeriesLineageResponse(s.getLineage().familyId(), s.getLineage().previousSeriesId(),
						s.getLineage().firstPosition(), s.getLineage().untilPosition()));
	}

}
