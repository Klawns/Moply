package com.klaus.moply.workorders.infra.web.dto.request;

import java.time.LocalDate;

import com.klaus.moply.workorders.application.usecase.dto.CreateWorkOrderInput;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateWorkOrderRequest(@NotNull LocalDate serviceDate, @NotNull @Valid WorkConditionsRequest conditions,
		String acceptedPricingFingerprint) {

	public CreateWorkOrderInput toInput() {
		return conditions.toInput(serviceDate, acceptedPricingFingerprint);
	}

}
