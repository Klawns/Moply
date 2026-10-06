package com.klaus.moply.recurrence.infra.web.dto.request;

import com.klaus.moply.recurrence.application.usecase.dto.CreateSeriesInput;
import com.klaus.moply.recurrence.domain.Frequency;
import com.klaus.moply.shared.infra.web.dto.request.DatePeriodRequest;
import com.klaus.moply.workorders.infra.web.dto.request.WorkConditionsRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateSeriesRequest(@NotNull Frequency frequency, @NotNull @Valid DatePeriodRequest period,
		@NotNull @Valid WorkConditionsRequest conditions, String acceptedPricingFingerprint) {

	public CreateSeriesInput toInput() {
		return new CreateSeriesInput(frequency, period.startsOn(), period.endsOn(),
				conditions.toInput(period.startsOn(), acceptedPricingFingerprint));
	}

}
